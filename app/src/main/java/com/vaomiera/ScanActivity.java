package com.vaomiera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

/**
 * Scanne le QR code de l'administrateur (cadre de visee + lampe) et renvoie l'IP (extra "ip").
 * L'ecran d'accueil se connecte ensuite automatiquement.
 */
public class ScanActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

	/** Cadre de visee style "scanner" : fond assombri, coins verts, ligne qui balaie. */
	static class Viseur extends View {
		private final Paint masque = new Paint();
		private final Paint coin = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint ligne = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final RectF cadre = new RectF();
		private final float d;
		private long debut = System.currentTimeMillis();

		Viseur(Context c) {
			super(c);
			d = c.getResources().getDisplayMetrics().density;
			masque.setColor(0x99000000);
			coin.setColor(0xFF00E676);
			coin.setStyle(Paint.Style.STROKE);
			coin.setStrokeWidth(4 * d);
			coin.setStrokeCap(Paint.Cap.ROUND);
			ligne.setColor(0xCC00E676);
			ligne.setStrokeWidth(2 * d);
		}

		RectF getCadre(int w, int h) {
			float cote = Math.min(w, h) * 0.68f;
			float cx = w / 2f;
			float cy = h * 0.42f;
			cadre.set(cx - cote / 2, cy - cote / 2, cx + cote / 2, cy + cote / 2);
			return cadre;
		}

		@Override
		protected void onDraw(Canvas c) {
			int w = getWidth();
			int h = getHeight();
			RectF r = getCadre(w, h);
			// fond assombri autour du cadre
			c.drawRect(0, 0, w, r.top, masque);
			c.drawRect(0, r.bottom, w, h, masque);
			c.drawRect(0, r.top, r.left, r.bottom, masque);
			c.drawRect(r.right, r.top, w, r.bottom, masque);
			// coins
			float l = 30 * d;
			c.drawLine(r.left, r.top, r.left + l, r.top, coin);
			c.drawLine(r.left, r.top, r.left, r.top + l, coin);
			c.drawLine(r.right, r.top, r.right - l, r.top, coin);
			c.drawLine(r.right, r.top, r.right, r.top + l, coin);
			c.drawLine(r.left, r.bottom, r.left + l, r.bottom, coin);
			c.drawLine(r.left, r.bottom, r.left, r.bottom - l, coin);
			c.drawLine(r.right, r.bottom, r.right - l, r.bottom, coin);
			c.drawLine(r.right, r.bottom, r.right, r.bottom - l, coin);
			// ligne qui balaie
			float t = ((System.currentTimeMillis() - debut) % 1800) / 1800f;
			float tri = t < 0.5f ? t * 2 : (1 - t) * 2;
			float y = r.top + 8 * d + tri * (r.height() - 16 * d);
			c.drawLine(r.left + 10 * d, y, r.right - 10 * d, y, ligne);
			postInvalidateDelayed(16);
		}
	}

	private SurfaceView surface;
	private Camera camera;
	private boolean surfacePrete = false;
	private boolean termine = false;
	private boolean lampe = false;
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
		racine.addView(new Viseur(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		TextView titre = new TextView(this);
		titre.setText("Scanner le QR code");
		titre.setTextColor(Color.WHITE);
		titre.setTextSize(20);
		titre.setGravity(Gravity.CENTER);
		FrameLayout.LayoutParams lpTitre = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpTitre.gravity = Gravity.TOP;
		lpTitre.topMargin = (int) (70 * getResources().getDisplayMetrics().density);
		racine.addView(titre, lpTitre);

		TextView aide = new TextView(this);
		aide.setText("Placez le QR code de l'administrateur dans le cadre.\nLa connexion se fait automatiquement.");
		aide.setTextColor(0xFFE0E0E0);
		aide.setTextSize(14);
		aide.setGravity(Gravity.CENTER);
		aide.setPadding(40, 0, 40, 0);
		FrameLayout.LayoutParams lpAide = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpAide.gravity = Gravity.BOTTOM;
		lpAide.bottomMargin = (int) (170 * getResources().getDisplayMetrics().density);
		racine.addView(aide, lpAide);

		Button btnLampe = new Button(this);
		btnLampe.setText("🔦 Lampe");
		btnLampe.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				basculerLampe();
			}
		});
		FrameLayout.LayoutParams lpLampe = new FrameLayout.LayoutParams((int) (150 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
		lpLampe.gravity = Gravity.BOTTOM | Gravity.START;
		lpLampe.setMargins(40, 0, 0, 60);
		racine.addView(btnLampe, lpLampe);

		Button btnFermer = new Button(this);
		btnFermer.setText("✕ Fermer");
		btnFermer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		FrameLayout.LayoutParams lpFermer = new FrameLayout.LayoutParams((int) (150 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
		lpFermer.gravity = Gravity.BOTTOM | Gravity.END;
		lpFermer.setMargins(0, 0, 40, 60);
		racine.addView(btnFermer, lpFermer);

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

	private void basculerLampe() {
		if (camera == null) return;
		try {
			Camera.Parameters p = camera.getParameters();
			List<String> modes = p.getSupportedFlashModes();
			if (modes == null || !modes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
				Toast.makeText(this, "Lampe indisponible", Toast.LENGTH_SHORT).show();
				return;
			}
			lampe = !lampe;
			p.setFlashMode(lampe ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
			camera.setParameters(p);
		} catch (Exception e) {}
	}

	private void demarrer() {
		if (camera != null || !surfacePrete || !permissionOk() || termine) return;
		try {
			camera = Camera.open();
			camera.setDisplayOrientation(90);
			Camera.Parameters p = camera.getParameters();
			// taille d'apercu proche du ratio de l'ecran (evite l'image etiree)
			DisplayMetrics dm = getResources().getDisplayMetrics();
			double ratioEcran = (double) Math.max(dm.widthPixels, dm.heightPixels) / Math.min(dm.widthPixels, dm.heightPixels);
			Camera.Size meilleure = null;
			double ecartMin = Double.MAX_VALUE;
			List<Camera.Size> tailles = p.getSupportedPreviewSizes();
			if (tailles != null) {
				for (Camera.Size t : tailles) {
					if (t.width > 1280 || t.width < 640) continue;
					double ecart = Math.abs((double) t.width / t.height - ratioEcran);
					if (ecart < ecartMin) {
						ecartMin = ecart;
						meilleure = t;
					}
				}
			}
			if (meilleure != null) p.setPreviewSize(meilleure.width, meilleure.height);
			List<String> modes = p.getSupportedFocusModes();
			if (modes != null && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
				p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			}
			camera.setParameters(p);
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
			lampe = false;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera cam) {
		if (termine) return;
		long maintenant = System.currentTimeMillis();
		if (maintenant - dernierDecodage < 200) return;
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
				if (ipsValides(texte)) {
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

	private boolean ipsValides(String ips) {
		String[] parts = ips.split(",");
		if (parts.length == 0) return false;
		for (int i = 0; i < parts.length; i++) {
			if (!parts[i].trim().matches("^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$")) return false;
		}
		return true;
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
