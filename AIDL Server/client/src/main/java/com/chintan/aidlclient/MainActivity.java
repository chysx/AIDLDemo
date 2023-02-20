package com.chintan.aidlclient;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chintan.aidlserver.IAdd;
import com.chintan.aidlserver.INumCallback;
import com.chintan.aidlserver.Person;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	private EditText num1, num2;
	private Button btnAdd, btnNonPremitive, btnCall, btnCallback;
	private TextView total;
	protected IAdd addService;
	private String Tag = "Client Application";
	private String serverAppUri = "com.chintan.aidlserver";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		num1 = (EditText) findViewById(R.id.num1);
		num2 = (EditText) findViewById(R.id.num2);
		btnAdd = (Button) findViewById(R.id.btnAdd);
		btnAdd.setOnClickListener(this);

		btnCall = (Button) findViewById(R.id.btnCall);
		btnCall.setOnClickListener(this);

		btnNonPremitive = (Button) findViewById(R.id.btnNonPremitive);
		btnNonPremitive.setOnClickListener(this);

		total = (TextView) findViewById(R.id.total);

		btnCallback = findViewById(R.id.btnCallBack);
		btnCallback.setOnClickListener(this);

		initConnection();
	}

	private void initConnection() {
		if (addService == null) {
			Intent intent = new Intent(IAdd.class.getName());

			/*this is service name which has been declared in the server's manifest file in service's intent-filter*/
			intent.setAction("service.calc");

			/*From 5.0 annonymous intent calls are suspended so replacing with server app's package name*/
			intent.setPackage("com.chintan.aidlserver");

			// binding to remote service
			bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
		}
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			Log.d(Tag, "Service Connected");
			addService = IAdd.Stub.asInterface((IBinder) iBinder);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.d(Tag, "Service Disconnected");
			addService = null;
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
	}

	private INumCallback mCallback = new INumCallback.Stub() {
		@Override
		public void call(final int num) throws RemoteException {
			/// 如果是在客户端主线程中注册的回调，那么该段代码会在主线程中执行，否则会在子线程中执行（可以试试）
			Log.e(Tag, Thread.currentThread().getName() + "  calculation" + num);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					total.setText(num + "");
				}
			});

		}
	};

	@Override
	public void onClick(View view) {
		Log.e(Tag, "onclick	当前线程" + Thread.currentThread().getName());
		if (appInstalledOrNot(serverAppUri)) {
			switch (view.getId()) {

				case R.id.btnAdd:

					if (num1.length() > 0 && num2.length() > 0 && addService != null) {
						try {
							total.setText("");
							total.setText("Result: " + addService
									.addNumbers(Integer.parseInt(num1.getText().toString()),
											Integer.parseInt(num2.getText().toString())));
							Log.e(Tag, "add 执行完成");


						} catch (RemoteException e) {
							e.printStackTrace();
							Log.d(Tag, "Connection cannot be establish");
						}
					}

					break;

				case R.id.btnNonPremitive:
					try {
						List<String> list = addService.getStringList();
						for (int i = 0; i < list.size(); i++) {
							Log.d("List Data: ", list.get(i));
						}

						List<Person> person = addService.getPersonList();
						total.setText("\n" + "Custom Object Data");
						for (int i = 0; i < person.size(); i++) {
							total.append(
									"\n" + "Person Data: " + "Name:" + person.get(i).name + " Age:" + person
											.get(i).age);
						}
					} catch (RemoteException e) {
						e.printStackTrace();
						Log.d(Tag, "Connection cannot be establish");
					}
					break;

				case R.id.btnCall:
					try {
						addService.placeCall("1234567890");
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					break;
				case R.id.btnCallBack:
					new Thread(){
						@Override
						public void run() {
							try {
								// 注册回调最好是在子线程中，因为服务端触发回调的时机客户端是不知道的
								// 因此这段代码需要放在子线程中，否则，会阻塞主线程（虽然回调具体的执行是在服务端的Binder线程池中执行，但是会在客户端的调用处阻塞客户端的主线程）
								addService.registerCallback(mCallback);
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}.start();
					Log.e(Tag, "callback 执行完成");
					break;
			}
		} else {
			Toast.makeText(MainActivity.this, "Server App not installed", Toast.LENGTH_SHORT).show();
		}
	}

	private boolean appInstalledOrNot(String uri) {
		PackageManager pm = getPackageManager();
		boolean app_installed;
		try {
			pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
			app_installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			app_installed = false;
		}
		return app_installed;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (addService == null) {
			initConnection();
		}
	}
}