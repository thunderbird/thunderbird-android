package net.londatiga.android;


import android.app.Activity;
import android.os.Bundle;

import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.Toast;

public class TestQuickAction extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		final ActionItem first = new ActionItem();

		first.setTitle("Dashboard");
		first.setIcon(getResources().getDrawable(R.drawable.dashboard));
		first.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(TestQuickAction.this, "Dashboard" , Toast.LENGTH_SHORT).show();
			}
		});


		final ActionItem second = new ActionItem();

		second.setTitle("Users & Groups");
		second.setIcon(getResources().getDrawable(R.drawable.kontak));
		second.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(TestQuickAction.this, "User & Group", Toast.LENGTH_SHORT).show();
			}
		});

		Button btn1 = (Button) this.findViewById(R.id.btn1);
		btn1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				QuickAction qa = new QuickAction(v);

				qa.addActionItem(first);
				qa.addActionItem(second);

				qa.show();
			}
		});

		Button btn2 = (Button) this.findViewById(R.id.btn2);
		btn2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				QuickAction qa = new QuickAction(v);

				qa.addActionItem(first);
				qa.addActionItem(second);
				qa.setAnimStyle(QuickAction.ANIM_REFLECT);

				qa.show();
			}
		});

		Button btn3 = (Button) this.findViewById(R.id.btn3);
		btn3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				QuickAction qa = new QuickAction(v);

				qa.addActionItem(first);
				qa.addActionItem(second);

				qa.show();
			}
		});

	}
}