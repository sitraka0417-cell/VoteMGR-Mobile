package com.vaomiera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Scanne le QR code de l'administrateur et renvoie l'adresse IP (extra "ip"). */
public class ScanActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

	private SurfaceView surface;
	private Camera camera;
	private boolean surfacePrete = false;
	private boolean termine = false;
	private long dernierDecodage = 0;
	private int largeur = 0;
	private int hauteur = 0;
	private final QRCodeReader lecteur = new QRCodeReader();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FrameLayout racine = new FrameLayout(this);
		racine.setBackgroundColor(Color.BLACK);
		surface = new SurfaceView(this);
		racine.addView(surface, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		TextView aide = new TextView(this);
		aide.setText("Placez le QR code de l'administrateur dans le cadre");
		aide.setTextColor(Color.WHITE);
		aide.setTextSize(16);
		aide.setGravity(Gravity.CENTER);
		aide.setBackgroundColor(0x99000000);
		aide.setPadding(30, 30, 30, 30);
		FrameLayout.LayoutParams lpAide = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpAide.gravity = Gravity.BOTTOM;
		racine.addView(aide, lpAide);
		setContentView(racine);
		surface.getHolder().addCallback(this);

		if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.CAMERA}, 77);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 77) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				demarrer();
			} else {
				Toast.makeText(this, "Autorisation de la caméra refusée", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	private boolean permissionOk() {
		return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
	}

	private void demarrer() {
		if (camera != null || !surfacePrete || !permissionOk() || termine) return;
		try {
			camera = Camera.open();
			camera.setDisplayOrientation(90);
			Camera.Parameters p = camera.getParameters();
			List<String> modes = p.getSupportedFocusModes();
			if (modes != null && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
				p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				camera.setParameters(p);
			}
			Camera.Size taille = camera.getParameters().getPreviewSize();
			largeur = taille.width;
			hauteur = taille.height;
			camera.setPreviewDisplay(surface.getHolder());
			camera.setPreviewCallback(this);
			camera.startPreview();
		} catch (Exception e) {
			Toast.makeText(this, "Caméra indisponible : " + e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private void arreter() {
		if (camera != null) {
			try {
				camera.setPreviewCallback(null);
				camera.stopPreview();
			} catch (Exception e) {}
			camera.release();
			camera = null;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera cam) {
		if (termine) return;
		long maintenant = System.currentTimeMillis();
		if (maintenant - dernierDecodage < 250) return;
		dernierDecodage = maintenant;
		try {
			PlanarYUVLuminanceSource src = new PlanarYUVLuminanceSource(data, largeur, hauteur, 0, 0, largeur, hauteur, false);
			BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(src));
			Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
			hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
			Result r = lecteur.decode(bmp, hints);
			String texte = r.getText();
			if (texte != null) {
				texte = texte.trim();
				if (texte.startsWith("VOTEMGR:")) texte = texte.substring(8).trim();
				if (texte.matches("^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$")) {
					termine = true;
					Intent res = new Intent();
					res.putExtra("ip", texte);
					setResult(RESULT_OK, res);
					finish();
				}
			}
		} catch (Exception e) {
			// pas de QR code dans cette image
		} finally {
			lecteur.reset();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		surfacePrete = true;
		demarrer();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		surfacePrete = false;
		arreter();
	}

	@Override
	protected void onPause() {
		super.onPause();
		arreter();
	}

	@Override
	protected void onResume() {
		super.onResume();
		demarrer();
	}
}
