#!/usr/bin/python

import logging, os, subprocess, sys, urllib, urllib2, zipfile, re, tempfile, shutil

sputnik_version='1.7.1'
sputnik_base_url='https://sputnik.ci/'

def configure_logger():
    root = logging.getLogger()
    root.setLevel(logging.DEBUG)

    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(logging.DEBUG)
    formatter = logging.Formatter('[%(levelname)s] %(asctime)s %(message)s')
    ch.setFormatter(formatter)
    root.addHandler(ch)


class CIVariables(object):
    def __init__(self, ci_service_name=None, ci=None, ci_name=None, pull_request_number=None, repo_slug=None, api_key=None, build_id=None):
        self.ci_service_name = ci_service_name
        self.ci = ci
        self.ci_name = ci_name
        self.pull_request_number = pull_request_number
        self.repo_slug = repo_slug
        self.api_key = api_key
        self.build_id = build_id

    def is_set_every_required_env(self):
        return self.ci_service_name is not None and self.ci is not None and self.ci_name is not None \
               and self.pull_request_number is not None and self.repo_slug is not None and (self.api_key is not None or self.build_id is not None)

    def is_pull_request_initiated(self):
        pull_request_initiated = self.ci == 'true' and self.ci_name == 'true' and self.pull_request_number != "false"
        if not pull_request_initiated:
            logging.error('Stop processing as pull request has not been initiated')
        return pull_request_initiated


def get_env(single_env):
    try:
        assert (os.environ[single_env])
        return os.environ[single_env]
    except Exception:
        logging.debug("Problem while reading env variable: " + single_env)
        return None


def detect_ci_service_name():
    if get_env('TRAVIS'):
        return 'TRAVIS'
    elif get_env('CIRCLECI'):
        return 'CIRCLECI'
    else:
        return None


def check_required_env_variables(required_vars):
    logging.info("Check required env variables: " + str(required_vars))
    for env_var in required_vars:
        if get_env(env_var) is None:
            logging.error("Env variable " + env_var + " is required to run sputnik")
            return False
    return True


def init_travis_variables(ci_variables):
    ci_variables.ci = get_env("CI")
    ci_variables.ci_name = get_env("TRAVIS")
    ci_variables.pull_request_number = get_env("TRAVIS_PULL_REQUEST")
    ci_variables.repo_slug = get_env("TRAVIS_REPO_SLUG")
    ci_variables.build_id = get_env("TRAVIS_BUILD_ID")


def get_circleci_pr_number(repo_slug):
    pr_from_fork = get_env("CIRCLE_PR_NUMBER")
    pr_number = None
    if pr_from_fork is None:
        pull_requests_str = get_env("CI_PULL_REQUESTS")
        if pull_requests_str is not None:
            pull_request_url_prefix = "https://github.com/" + repo_slug + "/pull/"
            pull_requests = list(map(lambda pr: int(pr[len(pull_request_url_prefix):]), pull_requests_str.split(",")))
            pr_number = max(pull_requests)
    else:
        pr_number = pr_from_fork
    return pr_number


def init_circleci_variables(ci_variables):
    ci_variables.ci = get_env("CI")
    ci_variables.ci_name = get_env("CIRCLECI")
    ci_variables.repo_slug = get_env("CIRCLE_PROJECT_USERNAME") + '/' + get_env("CIRCLE_PROJECT_REPONAME")
    ci_variables.pull_request_number = get_circleci_pr_number(ci_variables.repo_slug)
    ci_variables.build_id = get_env("CIRCLE_BUILD_NUM")


def init_variables():
    ci_variables = CIVariables()
    ci_variables.ci_service_name = detect_ci_service_name()
    if ci_variables.ci_service_name == 'TRAVIS':
        init_travis_variables(ci_variables)
    elif ci_variables.ci_service_name == 'CIRCLECI':
        init_circleci_variables(ci_variables)

    ci_variables.api_key = get_env("sputnik_api_key")
    return ci_variables


def unzip(zip):
    zip_ref = zipfile.ZipFile(zip, 'r')
    zip_ref.extractall(".")
    zip_ref.close()


def download_file(url, file_name):
    logging.info("Downloading " + file_name)
    try:
        urllib.urlretrieve(url, filename=file_name)
    except Exception:
        logging.error("Problem while downloading " + file_name + " from " + url)


def query_params(ci_variables):
    query_vars = {}
    query_vars['key'] = ci_variables.api_key
    query_vars['build_id'] = ci_variables.build_id
    return urllib.urlencode(dict((k, v) for k,v in query_vars.iteritems() if v is not None))


def are_credentials_correct(ci_variables):
    check_key_request = urllib2.Request(sputnik_base_url + "api/github/" + ci_variables.repo_slug + "/credentials?" + query_params(ci_variables))
    code = None
    try:
        response = urllib2.urlopen(check_key_request)
        code = response.code
    except urllib2.HTTPError as e:
        code = e.code
    return code == 200


def download_files_and_run_sputnik(ci_variables):
    if ci_variables.is_pull_request_initiated():
        if not are_credentials_correct(ci_variables):
            logging.error("API key or build id is incorrect. Please make sure that you passed correct value to CI settings.")
            return

        configs_url = sputnik_base_url + "conf/" + ci_variables.repo_slug + "/configs?" + query_params(ci_variables)
        download_file(configs_url, "configs.zip")
        unzip("configs.zip")

        # K-9 Mail hack - patch config
        replace("sputnik.properties", r'^checkstyle\.configurationFile=.*$', "checkstyle.configurationFile=config/checkstyle/checkstyle.xml")

        global sputnik_version
        sputnik_jar_url = "http://repo1.maven.org/maven2/pl/touk/sputnik/" + sputnik_version + "/sputnik-" + sputnik_version + "-all.jar"
        logging.debug('Sputnik jar url: ' + sputnik_jar_url)
        download_file(sputnik_jar_url, "sputnik.jar")

        sputnik_params = ['--conf', 'sputnik.properties', '--pullRequestId', str(ci_variables.pull_request_number)]
        if ci_variables.api_key is not None:
            sputnik_params = sputnik_params + ['--apiKey', ci_variables.api_key]
        if ci_variables.build_id is not None:
            sputnik_params = sputnik_params + ['--buildId', ci_variables.build_id]
        subprocess.call(['java', '-jar', 'sputnik.jar'] + sputnik_params)


def replace(file_path, pattern, subst):
    #Create temp file
    fh, abs_path = tempfile.mkstemp()
    with open(abs_path,'w') as new_file:
        with open(file_path) as old_file:
            for line in old_file:
                new_file.write(re.sub(pattern, subst, line))
    os.close(fh)
    #Remove original file
    os.remove(file_path)
    #Move new file
    shutil.move(abs_path, file_path)


def sputnik_ci():
    configure_logger()
    ci_variables = init_variables()

    if ci_variables.is_set_every_required_env():
        download_files_and_run_sputnik(ci_variables)
    else:
        logging.info("Env variables needed to run not set. Aborting.")

sputnik_ci()
