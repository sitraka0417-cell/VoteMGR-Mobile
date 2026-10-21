package com.vaomiera;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Color;
import android.util.TypedValue;
import android.content.Intent;
import android.widget.Toast;

public class MainActivity extends Activity {
	private EditText champIpRef;
	private EditText champNomRef;

	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		initializeLogic();
		Skin3D.apply(this);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		rootLayout.setPadding(50, 70, 50, 50);
		rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
		
		LogoView logo = new LogoView(this);
		rootLayout.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		TextView titre = new TextView(this);
		titre.setText("VoteMGR");
		titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
		titre.setTextColor(Color.parseColor("#222222"));
		titre.setPadding(0, 0, 0, 4);
		rootLayout.addView(titre);
		
		TextView sousTitreAuteur = new TextView(this);
		sousTitreAuteur.setText("by Sitraka Nambinintsoa");
		sousTitreAuteur.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		sousTitreAuteur.setTextColor(Color.parseColor("#999999"));
		sousTitreAuteur.setPadding(0, 0, 0, 60);
		rootLayout.addView(sousTitreAuteur);
		
		TextView labelNom = new TextView(this);
		labelNom.setText("Votre nom");
		labelNom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		rootLayout.addView(labelNom);
		
		final EditText champNom = new EditText(this);
		champNom.setHint("Entrez votre nom");
		rootLayout.addView(champNom);
		
		final LinearLayout blocParticipant = new LinearLayout(this);
		blocParticipant.setOrientation(LinearLayout.VERTICAL);
		blocParticipant.setVisibility(android.view.View.GONE);
		blocParticipant.setPadding(0, 30, 0, 0);
		
		TextView labelIp = new TextView(this);
		labelIp.setText("Adresse IP de l'administrateur");
		labelIp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		blocParticipant.addView(labelIp);
		
		final EditText champIp = new EditText(this);
		champIp.setHint("ex: 192.168.1.10");
		blocParticipant.addView(champIp);
		champIpRef = champIp;
		champNomRef = champNom;
		Button btnScan = new Button(this);
		btnScan.setText("📷 Scanner le QR de l'admin");
		btnScan.setOnClickListener(new android.view.View.OnClickListener() {
			public void onClick(android.view.View v) {
				startActivityForResult(new Intent(MainActivity.this, ScanActivity.class), 601);
			}
		});
		blocParticipant.addView(btnScan);
		
		rootLayout.addView(blocParticipant);
		
		Button btnAdmin = new Button(this);
		btnAdmin.setText("Je suis Administrateur");
		LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpBtn.setMargins(0, 60, 0, 20);
		btnAdmin.setLayoutParams(lpBtn);
		rootLayout.addView(btnAdmin);
		
		Button btnParticipant = new Button(this);
		btnParticipant.setText("Je suis Participant");
		LinearLayout.LayoutParams lpBtn2 = new LinearLayout.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpBtn2.setMargins(0, 0, 0, 20);
		btnParticipant.setLayoutParams(lpBtn2);
		rootLayout.addView(btnParticipant);
		
		final Button btnValider = new Button(this);
		btnValider.setText("Continuer");
		btnValider.setVisibility(android.view.View.GONE);
		rootLayout.addView(btnValider);
		
		setContentView(rootLayout);
		
		final String[] roleChoisi = {""};
		
		btnAdmin.setOnClickListener(new android.view.View.OnClickListener() {
				    public void onClick(android.view.View v) {
						        roleChoisi[0] = "admin";
						        blocParticipant.setVisibility(android.view.View.GONE);
						        btnValider.setVisibility(android.view.View.VISIBLE);
						        Toast.makeText(MainActivity.this, "Mode Administrateur sélectionné", Toast.LENGTH_SHORT).show();
						    }
		});
		
		btnParticipant.setOnClickListener(new android.view.View.OnClickListener() {
				    public void onClick(android.view.View v) {
						        roleChoisi[0] = "participant";
						        blocParticipant.setVisibility(android.view.View.VISIBLE);
						        btnValider.setVisibility(android.view.View.VISIBLE);
						        Toast.makeText(MainActivity.this, "Mode Participant sélectionné", Toast.LENGTH_SHORT).show();
						    }
		});
		
		btnValider.setOnClickListener(new android.view.View.OnClickListener() {
				    public void onClick(android.view.View v) {
						        String nom = champNom.getText().toString().trim();
						        if (nom.isEmpty()) {
								            Toast.makeText(MainActivity.this, "Entrez votre nom", Toast.LENGTH_SHORT).show();
								            return;
								        }
						        if (roleChoisi[0].equals("")) {
								            Toast.makeText(MainActivity.this, "Choisissez un rôle", Toast.LENGTH_SHORT).show();
								            return;
								        }
						        if (roleChoisi[0].equals("participant")) {
								            String ip = champIp.getText().toString().trim();
								            if (ip.isEmpty()) {
										                Toast.makeText(MainActivity.this, "Entrez l'adresse IP", Toast.LENGTH_SHORT).show();
										                return;
										            }
								            if (!ipsValides(ip)) {
											                Toast.makeText(MainActivity.this, "Adresse IP invalide (ex: 192.168.1.10)", Toast.LENGTH_LONG).show();
											                return;
											            }
								            Intent i = new Intent(MainActivity.this, VoteActivity.class);
								            i.putExtra("role", "participant");
								            i.putExtra("nom", nom);
								            i.putExtra("ip", ip);
								            startActivity(i);
								        } else {
								            Intent i = new Intent(MainActivity.this, AdminActivity.class);
								            i.putExtra("role", "admin");
								            i.putExtra("nom", nom);
								            startActivity(i);
								        }
						    }
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 601 && resultCode == RESULT_OK && data != null) {
			String ip = data.getStringExtra("ip");
			if (ip != null && champIpRef != null) {
				champIpRef.setText(ip);
				String nomScan = champNomRef != null ? champNomRef.getText().toString().trim() : "";
				if (nomScan.isEmpty()) {
					Toast.makeText(this, "IP détectée : " + ip + " — entrez votre nom puis « Continuer »", Toast.LENGTH_LONG).show();
				} else {
					Intent i = new Intent(MainActivity.this, VoteActivity.class);
					i.putExtra("role", "participant");
					i.putExtra("nom", nomScan);
					i.putExtra("ip", ip);
					startActivity(i);
				}
			}
		}
	}

	private boolean ipsValides(String ips) {
		String[] parts = ips.split(",");
		if (parts.length == 0) return false;
		for (int i = 0; i < parts.length; i++) {
			if (!parts[i].trim().matches("^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$")) return false;
		}
		return true;
	}

	private void initializeLogic() {
	}
	
}