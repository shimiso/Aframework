package com.eshangke.framework.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.eshangke.framework.MainApplication;
import com.eshangke.framework.R;
import com.eshangke.framework.ui.activities.MainActivity;
import com.eshangke.framework.ui.activities.VersionUpdateDialog;

import org.xutils.common.Callback;
import org.xutils.ex.HttpException;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * 后台静默下载apk
 */
public class ApkDownloadService extends BaseService {
	private static final int NOTIFY_ID = 01201;
	/** 下载进度条 **/
	private int progress;
	private NotificationManager mNotificationManager;

	/** 是否取消下载 true表示取消下载 **/
	private boolean canceled;
	/** 安装包url **/
	private String apkUrl = "";
	/** 安装包md5 **/
//	private String md5 = "";
	/** 下载包安装文件路径 **/
	private String saveFileName = "";
	/** 回调结果 **/
	private VersionUpdateDialog.ICallbackResult callback;
	/** 下载的Binder **/
	private DownloadBinder binder;
	private MainApplication app;
	/** 下载服务是否挂掉 true表示已经挂了，默认false **/
	private boolean serviceIsDestroy = false;
	private Context context;
	/** 下载完成后的apk文件 **/
	private File apkfile;
	private Callback.Cancelable downHandler;
	private Context mContext = this;
	/** 下载失败重试的次数 **/
	private int reDownCount = 0;

	@Override
	public IBinder onBind(Intent intent) {
		apkUrl = intent.getStringExtra("apkUrl");
//		md5 = intent.getStringExtra("md5");
		saveFileName = intent.getStringExtra("saveFileName");
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		binder = new DownloadBinder();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		stopForeground(true);// 这个不确定是否有作用
		app = (MainApplication) getApplication();
	}

	public class DownloadBinder extends Binder {
		public void start() {
			if (downHandler == null || downHandler.isCancelled()) {
				progress = 0;
				setUpNotification();
				// 下载
				canceled = false;
				downloadApk();
			}
		}

		public void cancel() {
			canceled = true;
		}

		public int getProgress() {
			return progress;
		}

		public boolean isCanceled() {
			return canceled;
		}

		public boolean serviceIsDestroy() {
			return serviceIsDestroy;
		}

		public void cancelNotification() {
			// 这里是用户界面手动取消，所以会经过activity的onDestroy();方法
			// 取消通知
			mNotificationManager.cancel(NOTIFY_ID);
		}

		public void addCallback(VersionUpdateDialog.ICallbackResult callback) {
			ApkDownloadService.this.callback = callback;
		}

		public void retryDownload() {
			downloadApk();
		}


	}

	// 通知栏
	Notification mNotification;

	/**
	 * 创建通知
	 */
	private void setUpNotification() {
		mNotification = new Notification(R.drawable.ic_launcher, "开始下载", System.currentTimeMillis());
		// 放置在"正在运行"栏目中
		mNotification.flags = Notification.FLAG_ONGOING_EVENT;

		RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.version_apkdown_notifi);
		contentView.setTextViewText(R.id.tv_progress, "新版本正在下载...");
		// 指定个性化视图
		mNotification.contentView = contentView;

		Intent intent = new Intent(this,MainActivity.class);
		// 下面两句是 在按home后，点击通知栏，返回之前activity 状态;
		// 有下面两句的话，假如service还在后台下载， 在点击程序图片重新进入程序时，直接到下载界面，相当于把程序MAIN 入口改了 - -
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// 指定内容意图
		mNotification.contentIntent = contentIntent;
		mNotificationManager.notify(NOTIFY_ID, mNotification);
	}

	/**
	 * 
	 * 下载完成.
	 * 
	 * @author 史明松
	 * @update 2014年6月26日 下午1:36:18
	 */
	private void downloadComplete() {
		// 点击后消失
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
		// 更新参数,注意flags要使用FLAG_UPDATE_CURRENT
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		// 指定内容意图
		mNotification.contentIntent = contentIntent;
		mNotificationManager.notify(NOTIFY_ID, mNotification);

		serviceIsDestroy = true;
		// 下载完了，cancelled也要设置
		canceled = true;
		stopSelf();// 停掉服务自身
	}

	/**
	 * 下载apk.
	 * 
	 * @author 史明松
	 * @update 2014年6月20日 下午7:21:38
	 */
	private void downloadApk() {
		RequestParams params = new RequestParams(apkUrl);
		params.setAutoResume(true);
		params.setAutoRename(true);
		params.setSaveFilePath(saveFileName);
		downHandler= x.http().get(params, new Callback.ProgressCallback<File>() {
			@Override
			public void onWaiting() {

			}

			@Override
			public void onStarted() {
			}

			@Override
			public void onLoading(long total, long current, boolean isDownloading) {
				int progress = (int) (((float) current / total) * 100);
				String message = "";
				if (progress < 100) {
					message = "已下载" + progress + "%";
				} else {
					message = "下载完成,请安装！";
				}
				callback.OnBackResult(progress, message);
				mNotification.contentView.setTextViewText(R.id.tv_progress, message);
				mNotification.contentView.setProgressBar(R.id.progressbar, 100, progress, false);
				mNotificationManager.notify(NOTIFY_ID, mNotification);
			}

			@Override
			public void onSuccess(File result) {
				apkfile = result;
				/*if (!md5.equalsIgnoreCase(getFileMD5(apkfile))) {
					apkfile.delete();
					mNotificationManager.cancel(NOTIFY_ID);
					callback.OnBackResult(null, VersionUpdateDialog.ICallbackResult.BACK_RESULT_FAILED);
					return;
				}*/
				downloadComplete();
				installApk();
				Toast.makeText(context, "下载完成！", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onError(Throwable ex, boolean isOnCallback) {
				if (ex instanceof HttpException) { // 网络错误
					HttpException httpEx = (HttpException) ex;
					int responseCode = httpEx.getCode();
					String responseMsg = httpEx.getMessage();
					String errorResult = httpEx.getResult();
					if (responseCode == 416) {
						apkfile = new File(saveFileName);
						/*if (md5.equalsIgnoreCase(getFileMD5(apkfile))) {
							downloadComplete();
							installApk();
							return;
						}*/
						apkfile.delete();
					}
				}

				/** 如果次数耗尽就终止下载服务，清除通知栏任务 **/
				if (reDownCount == 2) {
					// 下载完毕 取消通知
					mNotificationManager.cancel(NOTIFY_ID);
					Toast.makeText(context, "网络或服务器发生未知经常，下载失败！", Toast.LENGTH_LONG).show();
					stopSelf();// 停掉服务自身
					callback.OnBackResult(null, VersionUpdateDialog.ICallbackResult.BACK_RESULT_FAILED);
					return;
				}
				/** 如果下载失败程序就不断递归重试直到下载完成 **/
				reDownCount = reDownCount + 1;
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				downloadApk();
			}

			@Override
			public void onCancelled(CancelledException cex) {
			}

			@Override
			public void onFinished() {

			}
		});
	}

	/**
	 * 
	 * 安装apk .
	 * 
	 * @author 史明松
	 * @update 2014年6月20日 下午7:20:40
	 */
	private void installApk() {
		/** 如果文件为空或者不存在就重新开启下载 **/
		if (apkfile == null || !apkfile.exists()) {
			downloadApk();
			return;
		}
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
		mContext.startActivity(i);
		callback.OnBackResult(null, VersionUpdateDialog.ICallbackResult.BACK_RESULT_FINISH);
	}

	public String getFileMD5(File file) {
		if (!file.isFile()) {
			return null;
		}
		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return bytesToHexString(digest.digest());
	}

	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
}
