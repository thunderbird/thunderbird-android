===============  K9Email 修改日志 ===========
1. master分支：github开源 Android Studio项目转换成Eclipse项目，变动的类：
   a)位置：MessagingController.java
	(1) 注释行：     -2526 -3238 -3284 -3323 -5089 -5093 -5113
		 对应的方法： builder.setVisibility()
		 错误提示：   The method setVisibility(int) is undefined for the type NotificationCompat.Builder
		 环境：       Android5.0 API19
		 级别：{FUTURE FIX -后期修复} 
    (2) 注释行：    -5119
		 对应的方法：builder.setPublicVersion()
		 错误提示：The method setPublicVersion(Notification) is undefined for the type NotificationCompat.Builder
		 级别：{FUTURE FIX -后期修复} 

2. 增加拍照来添加附件功能，变动的类
	a)AndroidMainfest.xml
		增加拍照权限：<uses-permission android:name="android.permission.CAMERA" />
	b)string.xml
		<string name="pick_photo">相册</string>
		<string name="take_photo">拍照</string>
	c)MessageCompose.java
		(1) 变量：private static final int DIALOG_CHOOSE_ATTACHMENT_SOURCE = 5;
				  private static final int ACTIVITY_REQUEST_TAKE_ATTACHMENT = 2;
		(2) 方法内增加代码：
			[1]	onCreateDialog():
					case DIALOG_CHOOSE_ATTACHMENT_SOURCE: {
						final CharSequence[] item = { getString(R.string.pick_photo), getString(R.string.take_photo) };
						return new AlertDialog.Builder(this).setTitle(R.string.add_attachment_action).setItems(item, new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int id) {
								switch (id) {
								case 0:
									onAddAttachment2("*/*");
									break;
								case 1:
									Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
									startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_TAKE_ATTACHMENT);
									break;
								default:
									break;
								}
							}
						}).setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						}).create();
					}
			[2] onActivityResult()
					case ACTIVITY_REQUEST_TAKE_ATTACHMENT:
						addAttachmentsFromCapture(data);
						mDraftNeedsSaving = true;
						break;
		(3)	修改方法：
			onAddAttachment()
				showDialog(DIALOG_CHOOSE_ATTACHMENT_SOURCE);// onAddAttachment2("*/*");
		(4) 增加方法：
			[1]	private void addAttachmentsFromCapture(Intent data) {
					Bitmap bitmap = (Bitmap) data.getExtras().get("data");
					String name = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";
					File mFile = getFile(name);
					FileOutputStream mFileOutputStream = getOutputStream(mFile, name);
					bitmap.compress(CompressFormat.JPEG, 100, mFileOutputStream);
					bitmap.recycle();
					try {
						mFileOutputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					Uri uri = Uri.fromFile(mFile);
					addAttachment(uri);
				}

			[2]	private FileOutputStream getOutputStream(File mFile, String name) {
					FileOutputStream mFileOutputStream = null;
					if (mFile != null) {
						try {
							mFileOutputStream = new FileOutputStream(mFile);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					} else {
						try {
							mFileOutputStream = openFileOutput(name, Context.MODE_PRIVATE);
							mFile = new File(getFilesDir(), name);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
					return mFileOutputStream;
				}

			[3]	private File getFile(String name) {
					File mFile = null;
					String state = Environment.getExternalStorageState();
					if (Environment.MEDIA_MOUNTED.equals(state)) {
						mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AttachImg" + File.separator + name);
						if (!mFile.getParentFile().exists())
							mFile.getParentFile().mkdirs();
					}
					return mFile;
				}
3.接下去目标：附加打开功能优化。
	位置：MessageViewFragment.onClick(){onDownloadRemainder();//Button：下载完整附件}
	SingleMessageView onShowAttachments() 显示附件
	Intent { act=android.intent.action.VIEW dat=content://com.fsck.k9.attachmentprovider/49341c69-3728-4d74-89df-3390c804e60f/8/VIEW/image/png/QQ截图20150821123354.png typ=image/png flg=0x80001 }
	content://com.fsck.k9.attachmentprovider/49341c69-3728-4d74-89df-3390c804e60f/8/VIEW/image%2Fpng/QQ%E6%88%AA%E5%9B%BE20150821123354.png
	
	/storage/emulated/0/Android/data/com.fsck.k9/cache/attachments/log.txt
	/data/data/com.fsck.k9/cache/body1989742193.tmp
	/data/data/com.fsck.k9/databases/f7def8e9-840e-41fe-aa55-3214bd2b40ff.db_att/62
{配置项}
【DELETE】
		WelcomeMessage.java  L37 配置：模拟点击事件Next
		AccountSetupBasics.java  L108 配置：Email & Password； L94 配置：模拟点击事件Next
		AccountSetupName.java L42 配置：Name ；L75 配置：模拟点击事件Next
		  

