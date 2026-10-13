import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

import generate_changelog_json as generator
import migrate_changelog_to_json as migration

REPO_ROOT = Path(__file__).resolve().parents[2]


class ChangelogTest(unittest.TestCase):
    def test_migration_preserves_beta_history_and_normalizes_date(self):
        root = ET.fromstring(
            '<changelog><release version="16.0b1" versioncode="40" date="2025-12-8">'
            '<change>Fixed: A bug</change><change>New: A feature</change>'
            '</release></changelog>'
        )
        schema = migration.load_schema(REPO_ROOT / 'schemas/changelog-release.schema.json')
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            index = migration.migrate_changelog(root, output, schema)
            release_file = output / 'changelog_release_16_0b1.json'
            release = json.loads(release_file.read_text())
            migration.write_index_file(index, output)
            self.assertTrue(release_file.read_bytes().endswith(b'\n'))
            self.assertTrue((output / 'changelog_index.json').read_bytes().endswith(b'\n'))
            self.assertEqual([note['type'] for note in release['notes']], ['fixed', 'new'])
            self.assertEqual([note['text'] for note in release['notes']], ['Fixed: A bug', 'New: A feature'])

    def test_k9_migration_excludes_beta_versions(self):
        root = ET.fromstring(
            '<changelog><release version="10.0b1" versioncode="39021" date="2025-03-19">'
            '<change>Beta note</change></release>'
            '<release version="6.904" versioncode="39004" date="2024-06-27">'
            '<change>Stable note</change></release></changelog>'
        )
        schema = migration.load_schema(REPO_ROOT / 'schemas/changelog-release.schema.json')
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            index = migration.migrate_changelog(root, output, schema, exclude_beta=True)
            self.assertEqual([entry['version'] for entry in index['releases']], ['6.904'])
            self.assertFalse((output / 'changelog_release_10_0b1.json').exists())

    def test_generation_orders_versions_not_dates(self):
        index = {'schemaVersion': 1, 'releases': [
            {'version': '6.711', 'versioncode': 37011, 'date': '2023-10-04',
             'resourceName': 'changelog_release_6_711'},
            {'version': '6.603', 'versioncode': 36003, 'date': '2023-10-12',
             'resourceName': 'changelog_release_6_603'},
        ]}
        release = {'version': '6.712', 'versioncode': 37012, 'date': '2023-11-30'}
        generator.update_index(index, release, 'changelog_release_6_712')
        self.assertEqual([entry['version'] for entry in index['releases']], ['6.712', '6.711', '6.603'])

    def test_generation_places_final_after_release_candidates_and_orders_beta_numbers(self):
        index = {'schemaVersion': 1, 'releases': []}
        for version in ('5.200-RC2', '5.200', '5.200-RC1', '10.0b9', '10.0b10'):
            generator.update_index(index, {'version': version, 'versioncode': 1,
                                           'date': '2025-01-01'}, generator.to_resource_name(version))
        self.assertEqual([entry['version'] for entry in index['releases']],
                         ['10.0b10', '10.0b9', '5.200', '5.200-RC2', '5.200-RC1'])

    def test_beta_generation_filters_groups_and_preserves_other_index_entries(self):
        notes = {
            'release': {'releases': [
                {'version': '25.0b1', 'release_date': '2026-10-01'},
                {'version': '25.0b2', 'release_date': '2026-10-08'},
            ]},
            'notes': [
                {'group': 1, 'tag': 'new', 'note': 'First beta'},
                {'group': 2, 'tag': 'fixed', 'note': 'Second beta', 'issues': [123]},
            ],
        }
        index = {'schemaVersion': 1, 'releases': [
            {'version': '24.0b2', 'versioncode': 60, 'date': '2026-09-14',
             'resourceName': 'changelog_release_24_0b2'},
        ]}
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            # Check the shared generator functions used by render-notes for each release.
            for info in notes['release']['releases']:
                release = generator.extract_release(info['version'], 61, 'thunderbird', notes)
                generator.validate_json(
                    release,
                    generator.load_schema(REPO_ROOT / 'schemas/changelog-release.schema.json'),
                    info['version'],
                )
                index = generator.update_index(index, release, generator.write_release_file(release, output))
            generator.validate_json(
                index,
                generator.load_schema(REPO_ROOT / 'schemas/changelog-index.schema.json'),
                'index',
            )
            self.assertEqual([entry['version'] for entry in index['releases']],
                             ['25.0b2', '25.0b1', '24.0b2'])
            self.assertEqual(json.loads((output / 'changelog_release_25_0b1.json').read_text())['notes'][0]['text'],
                             'First beta')
            self.assertEqual(json.loads((output / 'changelog_release_25_0b2.json').read_text())['notes'][0]['issues'],
                             [123])
            generator.write_index_file(index, output)
            self.assertTrue((output / 'changelog_release_25_0b2.json').read_bytes().endswith(b'\n'))
            self.assertTrue((output / 'changelog_index.json').read_bytes().endswith(b'\n'))

    def test_main_generates_only_requested_beta_release(self):
        notes = {
            'release': {'releases': [
                {'version': '25.0b1', 'release_date': '2026-10-01'},
                {'version': '25.0b2', 'release_date': '2026-10-08'},
            ]},
            'notes': [
                {'group': 1, 'tag': 'new', 'note': 'First beta'},
                {'group': 2, 'tag': 'fixed', 'note': 'Second beta', 'issues': [123]},
            ],
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / 'app-thunderbird/src/beta/res/raw'
            output.mkdir(parents=True)
            (root / 'schemas').symlink_to(REPO_ROOT / 'schemas', target_is_directory=True)
            existing_index = {'schemaVersion': 1, 'releases': [
                {'version': '25.0b1', 'versioncode': 60, 'date': '2026-10-01',
                 'resourceName': 'changelog_release_25_0b1'},
            ]}
            (output / 'changelog_index.json').write_text(json.dumps(existing_index))
            previous_release = output / 'changelog_release_25_0b1.json'
            previous_data = {
                'schemaVersion': 1,
                'version': '25.0b1',
                'versioncode': 60,
                'date': '2026-10-01',
                'notes': [{'type': 'new', 'text': 'First beta'}],
            }
            previous_release.write_text(json.dumps(previous_data) + '\n')
            with patch.object(generator, '__file__', str(root / 'scripts/changelog/generate_changelog_json.py')), \
                    patch.object(generator, 'load_release_notes', return_value=notes), \
                    patch('sys.argv', ['generate_changelog_json.py', 'net.thunderbird.android.beta',
                                       '25.0b2', '61']):
                generator.main()

            index = json.loads((output / 'changelog_index.json').read_text())
            self.assertEqual([entry['version'] for entry in index['releases']],
                             ['25.0b2', '25.0b1'])
            self.assertEqual([entry['versioncode'] for entry in index['releases']], [61, 60])
            self.assertEqual(index['releases'][1], existing_index['releases'][0])
            self.assertEqual(json.loads(previous_release.read_text()), previous_data)
            second = json.loads((output / 'changelog_release_25_0b2.json').read_text())
            self.assertEqual([note['text'] for note in second['notes']], ['Second beta'])
            self.assertEqual(second['notes'][0]['issues'], [123])
            self.assertTrue((output / 'changelog_index.json').read_bytes().endswith(b'\n'))
            self.assertTrue((output / 'changelog_release_25_0b2.json').read_bytes().endswith(b'\n'))


if __name__ == '__main__':
    unittest.main()
