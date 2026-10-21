package com.vaomiera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Urne de vote isometrique dessinee en 3D (decoration de l'ecran d'accueil). */
public class LogoView extends View {

	private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path = new Path();

	public LogoView(Context c) {
		super(c);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		float d = getResources().getDisplayMetrics().density;
		setMeasuredDimension(resolveSize((int) (300 * d), widthSpec), (int) (132 * d));
	}

	private void quad(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		path.reset();
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);
		path.lineTo(x3, y3);
		path.lineTo(x4, y4);
		path.close();
		c.drawPath(path, p);
	}

	@Override
	protected void onDraw(Canvas c) {
		float d = getResources().getDisplayMetrics().density;
		float cx = getWidth() / 2f;
		float a = 48 * d;
		float b = 24 * d;
		float h = 52 * d;
		float topY = 36 * d;

		// ombre au sol
		p.setShader(null);
		p.setStyle(Paint.Style.FILL);
		p.setColor(0x26000000);
		c.drawOval(new RectF(cx - a * 1.45f, topY + h - b * 0.9f, cx + a * 1.45f, topY + h + b * 1.2f), p);

		// face gauche
		p.setShader(new LinearGradient(0, topY, 0, topY + h + b, 0xFF4C5DC9, 0xFF27358F, Shader.TileMode.CLAMP));
		quad(c, cx - a, topY, cx, topY + b, cx, topY + b + h, cx - a, topY + h);

		// face droite
		p.setShader(new LinearGradient(0, topY, 0, topY + h + b, 0xFF3040B0, 0xFF1A237E, Shader.TileMode.CLAMP));
		quad(c, cx, topY + b, cx + a, topY, cx + a, topY + h, cx, topY + b + h);

		// dessus
		p.setShader(new LinearGradient(cx - a, topY - b, cx + a, topY + b, 0xFFB3BEFF, 0xFF7C8DF0, Shader.TileMode.CLAMP));
		quad(c, cx, topY - b, cx + a, topY, cx, topY + b, cx - a, topY);
		p.setShader(null);

		// fente
		float dx = a * 0.5f;
		float dy = -b * 0.5f;
		float tx = a * 0.07f;
		float ty = b * 0.07f;
		p.setColor(0xFF0D1445);
		quad(c, cx - dx - tx, topY - dy - ty, cx + dx - tx, topY + dy - ty, cx + dx + tx, topY + dy + ty,
				cx - dx + tx, topY - dy + ty);

		// bulletin qui depasse
		float ax = cx - dx * 0.9f;
		float ay = topY - dy * 0.9f;
		float bx = cx + dx * 0.9f;
		float by = topY + dy * 0.9f;
		float hp = 26 * d;
		p.setColor(Color.WHITE);
		quad(c, ax, ay, bx, by, bx, by - hp, ax, ay - hp);

		// coche verte
		float mx = (ax + bx) / 2f;
		float my = (ay + by) / 2f - hp / 2f;
		p.setStyle(Paint.Style.STROKE);
		p.setStrokeWidth(3 * d);
		p.setStrokeCap(Paint.Cap.ROUND);
		p.setStrokeJoin(Paint.Join.ROUND);
		p.setColor(0xFF2E7D32);
		path.reset();
		path.moveTo(mx - 7 * d, my);
		path.lineTo(mx - 2 * d, my + 5 * d);
		path.lineTo(mx + 8 * d, my - 6 * d);
		c.drawPath(path, p);
		p.setStyle(Paint.Style.FILL);
	}
}
