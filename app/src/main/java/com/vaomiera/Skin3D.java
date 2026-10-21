package com.vaomiera;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.WeakHashMap;

/**
 * Habillage 3D (boutons en relief, cartes avec ombre, champs en creux).
 * Cette classe ne change AUCUNE logique : elle restyle seulement les vues
 * (y compris celles creees plus tard) apres leur ajout a l'ecran.
 */
public class Skin3D {

	static final int INDIGO_DARK = 0xFF1A237E;
	static final int INDIGO = 0xFF3949AB;
	static final int INDIGO_SOFT = 0xFF5C6BC0;

	private static final WeakHashMap<View, Boolean> DONE = new WeakHashMap<View, Boolean>();
	private static float density = 1f;

	static int dp(float v) {
		return Math.round(v * density);
	}

	// ------------------------------------------------------------------ entree

	public static void apply(final Activity a) {
		density = a.getResources().getDisplayMetrics().density;
		Window w = a.getWindow();
		if (Build.VERSION.SDK_INT >= 21) {
			w.setStatusBarColor(INDIGO_DARK);
			w.setNavigationBarColor(0xFFDCE2F7);
		}
		GradientDrawable fond = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{0xFFF4F6FF, 0xFFDCE2F7});
		w.setBackgroundDrawable(fond);

		final View decor = w.getDecorView();
		decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				walk(decor, false);
			}
		});
		decor.post(new Runnable() {
			@Override
			public void run() {
				walk(decor, false);
			}
		});
	}

	// ----------------------------------------------------------------- parcours

	private static void walk(View v, boolean onDark) {
		Boolean etat = DONE.get(v);
		boolean conteneurSombre;
		if (etat == null) {
			conteneurSombre = skinOne(v, onDark);
			DONE.put(v, Boolean.valueOf(conteneurSombre));
		} else {
			conteneurSombre = etat.booleanValue();
		}
		if (v instanceof ViewGroup) {
			ViewGroup g = (ViewGroup) v;
			boolean sombre = onDark || conteneurSombre;
			for (int i = 0; i < g.getChildCount(); i++) {
				walk(g.getChildAt(i), sombre);
			}
		}
	}

	/** Retourne true si la vue est un conteneur sombre (en-tete). */
	private static boolean skinOne(View v, boolean onDark) {
		if (v instanceof CompoundButton) {
			if (Build.VERSION.SDK_INT >= 21) {
				((CompoundButton) v).setButtonTintList(ColorStateList.valueOf(onDark ? Color.WHITE : INDIGO));
			}
			return false;
		}
		if (v instanceof Button) {
			styleButton((Button) v, onDark);
			return false;
		}
		if (v instanceof EditText) {
			styleEdit((EditText) v);
			return false;
		}
		if (v instanceof TextView) {
			styleText((TextView) v, onDark);
			return false;
		}
		if (v instanceof ViewGroup) {
			Drawable bg = v.getBackground();
			if (bg instanceof ColorDrawable) {
				int c = ((ColorDrawable) bg).getColor();
				if ((c >>> 24) < 255) {
					return false;
				}
				int rgb = c & 0xFFFFFF;
				if (rgb == 0xFFFFFF) {
					v.setBackground(null); // laisse voir le degrade du fond
					return false;
				}
				if (rgb == 0xE8EAF6) {
					styleHeader(v);
					return true;
				}
				if (rgb == 0xFFF3E0) {
					stylePanel(v);
					return false;
				}
				styleCard(v, c);
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ couleurs

	private static int mix(int c, int target, float f) {
		int r = (int) (Color.red(c) + (Color.red(target) - Color.red(c)) * f);
		int g = (int) (Color.green(c) + (Color.green(target) - Color.green(c)) * f);
		int b = (int) (Color.blue(c) + (Color.blue(target) - Color.blue(c)) * f);
		return Color.argb(Color.alpha(c), r, g, b);
	}

	private static int lighten(int c, float f) {
		return mix(c, Color.WHITE, f);
	}

	private static int darken(int c, float f) {
		return mix(c, Color.BLACK, f);
	}

	// ------------------------------------------------------------------- boutons

	private static Drawable boutonCouche(int haut, int bas, int bord, int rayon, int profondeur, int decalage) {
		GradientDrawable edge = new GradientDrawable();
		edge.setColor(bord);
		edge.setCornerRadius(rayon);
		GradientDrawable face = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{haut, bas});
		face.setCornerRadius(rayon);
		LayerDrawable ld = new LayerDrawable(new Drawable[]{edge, face});
		ld.setLayerInset(1, 0, decalage, 0, profondeur - decalage);
		return ld;
	}

	private static void styleOutil(Button b, boolean onDark) {
		int base = onDark ? 0xFFFFFFFF : 0xFFE8EAF6;
		int bord = 0xFFAEB7E6;
		int rayon = dp(14);
		int prof = dp(4);
		StateListDrawable sl = new StateListDrawable();
		sl.addState(new int[]{android.R.attr.state_pressed}, boutonCouche(base, darken(base, 0.06f), bord, rayon, prof, dp(2)));
		sl.addState(new int[]{}, boutonCouche(base, darken(base, 0.03f), bord, rayon, prof, 0));
		b.setBackground(sl);
		b.setPadding(dp(10), dp(6), dp(10), dp(6) + prof);
		b.setMinWidth(dp(68));
		b.setMinimumWidth(dp(68));
		b.setMinHeight(dp(56));
		b.setMinimumHeight(dp(56));
		b.setAllCaps(false);
		b.setSingleLine(false);
		b.setMaxLines(2);
		b.setGravity(android.view.Gravity.CENTER);
		b.setTypeface(null, Typeface.BOLD);
		b.setTextColor(INDIGO_DARK);
		b.setShadowLayer(0f, 0f, 0f, 0);
		if (Build.VERSION.SDK_INT >= 21) {
			b.setStateListAnimator(null);
		}
	}

	private static void styleMini(Button b) {
		int base = 0xFFE53935;
		int rayon = dp(12);
		int prof = dp(4);
		StateListDrawable sl = new StateListDrawable();
		sl.addState(new int[]{android.R.attr.state_pressed}, boutonCouche(lighten(base, 0.18f), base, darken(base, 0.32f), rayon, prof, dp(2)));
		sl.addState(new int[]{}, boutonCouche(lighten(base, 0.18f), base, darken(base, 0.32f), rayon, prof, 0));
		b.setBackground(sl);
		b.setPadding(dp(6), dp(4), dp(6), dp(4) + prof);
		b.setMinWidth(dp(46));
		b.setMinimumWidth(dp(46));
		b.setMinHeight(dp(44));
		b.setMinimumHeight(dp(44));
		b.setAllCaps(false);
		b.setTextColor(Color.WHITE);
		b.setTextSize(16);
		if (Build.VERSION.SDK_INT >= 21) {
			b.setStateListAnimator(null);
		}
	}

	private static void styleButton(Button b, boolean onDark) {
		if ("mini".equals(b.getTag())) {
			styleMini(b);
			return;
		}
		if ("tool".equals(b.getTag())) {
			styleOutil(b, onDark);
			return;
		}
		String t = String.valueOf(b.getText()).toLowerCase();
		int base;
		boolean clair = false;
		if (onDark) {
			base = 0xFFFFFFFF;
			clair = true;
		} else if (t.contains("supprimer") || t.contains("\uD83D\uDDD1") || t.contains("effacer")) {
			base = 0xFFE53935;
		} else if (t.contains("retour") || t.contains("masquer") || t.contains("voir")
				|| t.contains("\u2B05") || t.contains("\u25BC") || t.contains("\u25B2") || t.contains("annuler")) {
			base = 0xFFE8EAF6;
			clair = true;
		} else if (t.contains("fermer") || t.contains("cl\u00F4tur") || t.contains("second tour")) {
			base = 0xFFEF6C00;
		} else if (t.contains("ouvrir") || t.contains("valider") || t.contains("cr\u00E9er")
				|| t.contains("\u2705") || t.contains("continuer")) {
			base = 0xFF2E7D32;
		} else {
			base = INDIGO;
		}

		int haut = lighten(base, clair ? 0.0f : 0.18f);
		int bas = clair ? darken(base, 0.04f) : base;
		int bord = clair ? 0xFFAEB7E6 : darken(base, 0.32f);
		int rayon = dp(14);
		int prof = dp(5);

		StateListDrawable sl = new StateListDrawable();
		sl.addState(new int[]{android.R.attr.state_pressed},
				boutonCouche(haut, bas, bord, rayon, prof, dp(3)));
		sl.addState(new int[]{-android.R.attr.state_enabled},
				boutonCouche(0xFFE0E0E0, 0xFFD0D0D0, 0xFFB0B0B0, rayon, prof, 0));
		sl.addState(new int[]{}, boutonCouche(haut, bas, bord, rayon, prof, 0));

		b.setBackground(sl);
		b.setPadding(dp(16), dp(10), dp(16), dp(10) + prof);
		b.setMinHeight(dp(50));
		b.setMinimumHeight(dp(50));
		b.setAllCaps(false);
		b.setTypeface(null, Typeface.BOLD);
		b.setTextColor(clair ? INDIGO_DARK : Color.WHITE);
		if (!clair) {
			b.setShadowLayer(1.5f, 0f, 2f, 0x66000000);
		}
		if (Build.VERSION.SDK_INT >= 21) {
			b.setStateListAnimator(null);
		}
	}

	// -------------------------------------------------------------------- champs

	private static Drawable champCouche(int bord) {
		int r = dp(12);
		GradientDrawable edge = new GradientDrawable();
		edge.setColor(bord);
		edge.setCornerRadius(r);
		GradientDrawable face = new GradientDrawable();
		face.setColor(Color.WHITE);
		face.setCornerRadius(r);
		LayerDrawable ld = new LayerDrawable(new Drawable[]{edge, face});
		ld.setLayerInset(1, dp(1), dp(3), dp(1), dp(1)); // bord haut plus epais = effet "en creux"
		return ld;
	}

	private static void styleEdit(EditText e) {
		StateListDrawable sl = new StateListDrawable();
		sl.addState(new int[]{android.R.attr.state_focused}, champCouche(INDIGO_SOFT));
		sl.addState(new int[]{}, champCouche(0xFFB8C0E8));
		e.setBackground(sl);
		e.setPadding(dp(14), dp(12), dp(14), dp(12));
		e.setTextColor(0xFF212121);
		e.setHintTextColor(0xFF9096B0);
	}

	// -------------------------------------------------------------------- textes

	private static void styleText(TextView t, boolean onDark) {
		float sp = t.getTextSize() / t.getResources().getDisplayMetrics().scaledDensity;
		if (onDark) {
			if (sp >= 20f) {
				t.setTextColor(Color.WHITE);
				t.setShadowLayer(1.5f, 0f, 3f, 0x66000000);
			} else {
				t.setTextColor(0xFFD5DBFF);
			}
			return;
		}
		if (sp >= 26f) { // grand titre : texte extrude en 3D
			t.setTypeface(null, Typeface.BOLD);
			t.setTextColor(INDIGO_DARK);
			t.setShadowLayer(0.5f, dp(2), dp(3), 0x663949AB);
		}
	}

	// ------------------------------------------------------------------- conteneurs

	private static void garderPadding(View v, Drawable d, float elevation, int extraBas) {
		int l = v.getPaddingLeft();
		int t = v.getPaddingTop();
		int r = v.getPaddingRight();
		int b = v.getPaddingBottom();
		v.setBackground(d);
		v.setPadding(l, t, r, b + extraBas);
		if (Build.VERSION.SDK_INT >= 21) {
			v.setElevation(elevation);
		}
	}

	private static void styleHeader(View v) {
		float r = dp(26);
		boolean enBas = false;
		if (v.getParent() instanceof ViewGroup) {
			ViewGroup pere = (ViewGroup) v.getParent();
			int idx = pere.indexOfChild(v);
			enBas = idx > 0 && idx == pere.getChildCount() - 1;
		}
		GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{0xFF3F51B5, INDIGO_DARK});
		if (enBas) {
			g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
		} else {
			g.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
		}
		garderPadding(v, g, dp(8), 0);
	}

	private static void stylePanel(View v) {
		float r = dp(22);
		float[] rayons = new float[]{r, r, r, r, 0, 0, 0, 0};
		GradientDrawable edge = new GradientDrawable();
		edge.setColor(0xFFFFB74D);
		edge.setCornerRadii(rayons);
		GradientDrawable face = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{0xFFFFFBF2, 0xFFFFE9C7});
		face.setCornerRadii(rayons);
		LayerDrawable ld = new LayerDrawable(new Drawable[]{edge, face});
		ld.setLayerInset(1, 0, dp(3), 0, 0);
		garderPadding(v, ld, dp(14), 0);
	}

	private static void styleCard(View v, int couleur) {
		int rayon = dp(18);
		int prof = dp(4);
		int bord = darken(couleur, 0.18f);
		GradientDrawable edge = new GradientDrawable();
		edge.setColor(bord);
		edge.setCornerRadius(rayon);
		GradientDrawable face = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{lighten(couleur, 0.65f), lighten(couleur, 0.15f)});
		face.setCornerRadius(rayon);
		LayerDrawable ld = new LayerDrawable(new Drawable[]{edge, face});
		ld.setLayerInset(1, 0, 0, 0, prof);
		garderPadding(v, ld, dp(5), prof);
	}
}
