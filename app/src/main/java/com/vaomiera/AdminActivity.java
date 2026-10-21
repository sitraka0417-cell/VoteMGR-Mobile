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
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.View;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.util.TypedValue;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Toast;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.text.format.Formatter;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;

public class AdminActivity extends Activity {
	private byte[] pdfEnAttente = null;
	private Runnable diffuserEtat = null;

	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.admin);
		initialize(_savedInstanceState);
		initializeLogic();
		Skin3D.apply(this);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		final LinearLayout screenContainer = new LinearLayout(this);
		screenContainer.setOrientation(LinearLayout.VERTICAL);
		setContentView(screenContainer);
		
		final ArrayList<HashMap<String, Object>> sections = new ArrayList<HashMap<String, Object>>();
		final ArrayList<HashMap<String, Object>> sessions = new ArrayList<HashMap<String, Object>>();
		final String[] nomProjet = {""};
		final HashMap<String, Object>[] sousSectionOuverte = new HashMap[]{null};
		final String nomAdmin = getIntent().getStringExtra("nom");
		
		String ipTrouvee = "Introuvable";
final ArrayList<String> ipsListe = new ArrayList<String>();
		try {
				    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				    while (interfaces.hasMoreElements()) {
						        NetworkInterface iface = interfaces.nextElement();
						        String nomIface = iface.getName();
						        if (!iface.isUp() || iface.isLoopback()) continue;
						        Enumeration<InetAddress> adresses = iface.getInetAddresses();
						        while (adresses.hasMoreElements()) {
								            InetAddress addr = adresses.nextElement();
								            if (addr.isLoopbackAddress()) continue;
								            String ip = addr.getHostAddress();
								            if (ip.indexOf(':') == -1) {
										                if (nomIface.contains("wlan") || nomIface.contains("ap") || nomIface.contains("swlan")) {
												                    ipTrouvee = ip;
if (!ipsListe.contains(ip)) ipsListe.add(ip);
												                }
										            }
								        }
						    }
		} catch (Exception e) {}
		final String ipLocale = ipTrouvee;
String ipsJointes = "";
for (int ii = 0; ii < ipsListe.size(); ii++) ipsJointes += (ii > 0 ? "," : "") + ipsListe.get(ii);
final String ipsLocales = ipsJointes.isEmpty() ? ipTrouvee : ipsJointes;
		
		class Ecrans {
				
				    LinearLayout containerActif;
				    Button btnVoterSticky;
				    ScrollView scrollPrincipal;
				    boolean scrollVersBasDemande = false;
				    CopyOnWriteArrayList<PrintWriter> clients = new CopyOnWriteArrayList<PrintWriter>();
				
				    String joinListe(ArrayList<String> l) {
						        String r = "";
						        for (int i = 0; i < l.size(); i++) {
								            if (i > 0) r += ", ";
								            r += l.get(i);
								        }
						        return r;
						    }
				
				    				    // ================= NOUVEAUTES : numerotation, consultation, elimination, PDF, QR =================
				    boolean enConsultation = false;

				    String romain(int n) {
				        String[] r = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
				        int[] v = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
				        String s = "";
				        for (int i = 0; i < v.length; i++) {
				            while (n >= v[i]) { s += r[i]; n -= v[i]; }
				        }
				        return s;
				    }

				    String badgeDe(HashMap<String, Object> u) {
				        String st = (String) u.get("statut");
				        if (st == null) return "";
				        if (st.equals("ouvert")) return "🟢 OUVERT";
				        if (st.equals("designe")) return "📌 DÉSIGNÉ";
				        if (st.equals("ferme")) return estResolu(u) ? "🔴 FERMÉ" : "⚖ DÉPARTAGE";
				        return "⚪ EN ATTENTE";
				    }

				    ArrayList<String> actifs(HashMap<String, Object> unite) {
				        ArrayList<String> cands = (ArrayList<String>) unite.get("candidats");
				        ArrayList<HashMap<String, Object>> elim = (ArrayList<HashMap<String, Object>>) unite.get("elimines");
				        if (elim == null || elim.size() == 0) return cands;
				        ArrayList<String> res = new ArrayList<String>();
				        for (int i = 0; i < cands.size(); i++) {
				            boolean elimine = false;
				            for (int j = 0; j < elim.size(); j++) {
				                if (cands.get(i).equals((String) elim.get(j).get("nom"))) { elimine = true; break; }
				            }
				            if (!elimine) res.add(cands.get(i));
				        }
				        return res;
				    }

				    boolean estResolu(HashMap<String, Object> u) {
				        String st = (String) u.get("statut");
				        if (st == null) return false;
				        if (st.equals("designe")) return true;
				        if (!st.equals("ferme")) return false;
				        if (Boolean.TRUE.equals(u.get("modeRoles"))) return resoudreRoles(u) != null;
				        return calculerElus(u) != null;
				    }

				    boolean uniteActive(HashMap<String, Object> u) {
				        String st = (String) u.get("statut");
				        if (st == null) return false;
				        if (st.equals("ouvert")) return true;
				        if (st.equals("ferme") && !estResolu(u)) return true;
				        return false;
				    }

				    boolean sectionActive(HashMap<String, Object> section) {
				        if (uniteActive(section)) return true;
				        ArrayList<HashMap<String, Object>> ss = (ArrayList<HashMap<String, Object>>) section.get("sousSections");
				        if (ss != null) {
				            for (int i = 0; i < ss.size(); i++) if (uniteActive(ss.get(i))) return true;
				        }
				        return false;
				    }

				    boolean sectionPlie(HashMap<String, Object> section) {
				        Object o = section.get("plie");
				        if (o != null) return ((Boolean) o).booleanValue();
				        return !sectionActive(section);
				    }

				    ArrayList<String> nomsElus(HashMap<String, Object> unite) {
				        ArrayList<String> noms = new ArrayList<String>();
				        String st = (String) unite.get("statut");
				        if ("designe".equals(st)) {
				            ArrayList<String> d = (ArrayList<String>) unite.get("designes");
				            if (d != null) noms.addAll(d);
				            return noms;
				        }
				        if (!"ferme".equals(st)) return null;
				        if (Boolean.TRUE.equals(unite.get("modeRoles"))) {
				            ArrayList<Object[]> a = resoudreRoles(unite);
				            if (a == null) return null;
				            for (int i = 0; i < a.size(); i++) noms.add((String) a.get(i)[0]);
				            return noms;
				        }
				        ArrayList<Object[]> e = calculerElus(unite);
				        if (e == null) return null;
				        for (int i = 0; i < e.size(); i++) noms.add((String) e.get(i)[0]);
				        return noms;
				    }

				    ArrayList<String> ensembleEgalite(HashMap<String, Object> unite) {
				        ArrayList<String> res = new ArrayList<String>();
				        ArrayList<String> candidats = actifs(unite);
				        HashMap<String, Integer> votes = (HashMap<String, Integer>) unite.get("votes");
				        int nbAdmis = unite.containsKey("nbAdmis") ? (Integer) unite.get("nbAdmis") : (Integer) unite.get("nbMax");
				        if (nbAdmis > candidats.size()) nbAdmis = candidats.size();
				        if (nbAdmis <= 0 || candidats.size() == 0) return res;
				        ArrayList<Object[]> tries = new ArrayList<Object[]>();
				        for (int i = 0; i < candidats.size(); i++) {
				            String nom = candidats.get(i);
				            int score = votes.containsKey(nom) ? votes.get(nom) : 0;
				            tries.add(new Object[]{nom, score});
				        }
				        for (int i = 0; i < tries.size(); i++) {
				            for (int j = i + 1; j < tries.size(); j++) {
				                if ((Integer) tries.get(j)[1] > (Integer) tries.get(i)[1]) {
				                    Object[] tmp = tries.get(i); tries.set(i, tries.get(j)); tries.set(j, tmp);
				                }
				            }
				        }
				        int seuil = (Integer) tries.get(nbAdmis - 1)[1];
				        int clairs = 0;
				        ArrayList<String> exAequo = new ArrayList<String>();
				        for (int i = 0; i < tries.size(); i++) {
				            int score = (Integer) tries.get(i)[1];
				            if (score > seuil) clairs++;
				            else if (score == seuil) exAequo.add((String) tries.get(i)[0]);
				        }
				        int restantes = nbAdmis - clairs;
				        if (restantes > 0 && restantes < exAequo.size()) res = exAequo;
				        return res;
				    }

				    void apresChangementElimination(HashMap<String, Object> unite) {
				        boolean roles = Boolean.TRUE.equals(unite.get("modeRoles"));
				        if (unite.containsKey("tourSuivant")) {
				            boolean garder = false;
				            if (!roles) {
				                HashMap<String, Object> tour = (HashMap<String, Object>) unite.get("tourSuivant");
				                ArrayList<String> ct = (ArrayList<String>) tour.get("candidats");
				                ArrayList<String> nouvelle = ensembleEgalite(unite);
				                garder = ct.size() == nouvelle.size() && ct.containsAll(nouvelle);
				            }
				            if (!garder) unite.remove("tourSuivant");
				        }
				        if (roles) resoudreRoles(unite);
				        else detecterEgalite(unite);
				    }

				    void eliminerElu(HashMap<String, Object> unite, String nom, String cause) {
				        ArrayList<String> avant = nomsElus(unite);
				        ArrayList<HashMap<String, Object>> elim = (ArrayList<HashMap<String, Object>>) unite.get("elimines");
				        if (elim == null) {
				            elim = new ArrayList<HashMap<String, Object>>();
				            unite.put("elimines", elim);
				        }
				        HashMap<String, Object> e = new HashMap<String, Object>();
				        e.put("nom", nom);
				        e.put("cause", cause);
				        elim.add(e);
				        apresChangementElimination(unite);
				        ArrayList<String> apres = nomsElus(unite);
				        String remplacants = "";
				        if (apres != null && avant != null) {
				            for (int i = 0; i < apres.size(); i++) {
				                if (!avant.contains(apres.get(i))) {
				                    if (remplacants.length() > 0) remplacants += ", ";
				                    remplacants += apres.get(i);
				                }
				            }
				        }
				        e.put("remplacant", remplacants);
				    }

				    void dialogEliminer(final HashMap<String, Object> unite, final ArrayList<String> elus) {
				        if (sousSectionOuverte[0] != null) {
				            Toast.makeText(AdminActivity.this, "Fermez d'abord le vote en cours", Toast.LENGTH_SHORT).show();
				            return;
				        }
				        LinearLayout l = new LinearLayout(AdminActivity.this);
				        l.setOrientation(LinearLayout.VERTICAL);
				        l.setPadding(40, 20, 40, 20);
				        TextView q = new TextView(AdminActivity.this);
				        q.setText("Quelle personne éliminer ?");
				        q.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				        l.addView(q);
				        final RadioGroup groupe = new RadioGroup(AdminActivity.this);
				        for (int i = 0; i < elus.size(); i++) {
				            RadioButton rb = new RadioButton(AdminActivity.this);
				            rb.setText(elus.get(i));
				            rb.setId(1000 + i);
				            groupe.addView(rb);
				        }
				        l.addView(groupe);
				        final EditText cause = new EditText(AdminActivity.this);
				        cause.setHint("Cause de l'élimination (obligatoire)");
				        cause.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
				        cause.setMinLines(2);
				        l.addView(cause);
				        ScrollView sv = new ScrollView(AdminActivity.this);
				        sv.addView(l);
				        new AlertDialog.Builder(AdminActivity.this)
				            .setTitle("Éliminer un élu")
				            .setView(sv)
				            .setPositiveButton("Éliminer", new DialogInterface.OnClickListener() {
				                public void onClick(DialogInterface dialog, int which) {
				                    int id = groupe.getCheckedRadioButtonId();
				                    String c = cause.getText().toString().trim();
				                    if (id < 1000 || id - 1000 >= elus.size()) {
				                        Toast.makeText(AdminActivity.this, "Choisissez la personne à éliminer", Toast.LENGTH_SHORT).show();
				                        return;
				                    }
				                    if (c.isEmpty()) {
				                        Toast.makeText(AdminActivity.this, "Indiquez la cause de l'élimination", Toast.LENGTH_SHORT).show();
				                        return;
				                    }
				                    eliminerElu(unite, elus.get(id - 1000), c);
				                    redessiner();
				                    broadcastEtat();
				                    Toast.makeText(AdminActivity.this, "Élimination enregistrée", Toast.LENGTH_SHORT).show();
				                }
				            })
				            .setNegativeButton("Annuler", null)
				            .show();
				    }

				    void ajouterBlocElimination(LinearLayout box, final HashMap<String, Object> unite) {
				        final ArrayList<HashMap<String, Object>> elim = (ArrayList<HashMap<String, Object>>) unite.get("elimines");
				        if (elim != null) {
				            for (int i = 0; i < elim.size(); i++) {
				                final int index = i;
				                HashMap<String, Object> e = elim.get(i);
				                String rempl = (String) e.get("remplacant");
				                TextView t = new TextView(AdminActivity.this);
				                t.setText("      🚫 " + e.get("nom") + " — " + e.get("cause")
				                    + ((rempl != null && rempl.length() > 0) ? "  →  remplacé(e) par " + rempl : ""));
				                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
				                t.setTextColor(Color.parseColor("#B71C1C"));
				                box.addView(t);
				                Button btnR = new Button(AdminActivity.this);
				                btnR.setText("↩ Rétablir " + e.get("nom"));
				                btnR.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
				                btnR.setOnClickListener(new View.OnClickListener() {
				                    public void onClick(View v) {
				                        if (sousSectionOuverte[0] != null) {
				                            Toast.makeText(AdminActivity.this, "Fermez d'abord le vote en cours", Toast.LENGTH_SHORT).show();
				                            return;
				                        }
				                        elim.remove(index);
				                        apresChangementElimination(unite);
				                        redessiner();
				                        broadcastEtat();
				                    }
				                });
				                box.addView(btnR);
				            }
				        }
				        final ArrayList<String> elus = nomsElus(unite);
				        if (elus != null && elus.size() > 0 && !"designe".equals(unite.get("statut"))) {
				            Button btnElim = new Button(AdminActivity.this);
				            btnElim.setText("🚫 Éliminer un élu");
				            btnElim.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
				            btnElim.setOnClickListener(new View.OnClickListener() {
				                public void onClick(View v) {
				                    dialogEliminer(unite, elus);
				                }
				            });
				            box.addView(btnElim);
				        }
				    }

				    void afficherQrConnexion() {
				        if ("Introuvable".equals(ipLocale)) {
				            Toast.makeText(AdminActivity.this, "IP introuvable : activez le Wi-Fi ou le point d'accès", Toast.LENGTH_LONG).show();
				            return;
				        }
				        try {
				            com.google.zxing.common.BitMatrix m = new com.google.zxing.qrcode.QRCodeWriter()
				                .encode("VOTEMGR:" + ipsLocales, com.google.zxing.BarcodeFormat.QR_CODE, 600, 600);
				            int w = m.getWidth();
				            int h = m.getHeight();
				            int[] px = new int[w * h];
				            for (int y = 0; y < h; y++) {
				                for (int x = 0; x < w; x++) px[y * w + x] = m.get(x, y) ? 0xFF1A237E : 0xFFFFFFFF;
				            }
				            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
				            bmp.setPixels(px, 0, w, 0, 0, w, h);
				            LinearLayout l = new LinearLayout(AdminActivity.this);
				            l.setOrientation(LinearLayout.VERTICAL);
				            l.setGravity(Gravity.CENTER_HORIZONTAL);
				            l.setPadding(40, 30, 40, 20);
				            ImageView iv = new ImageView(AdminActivity.this);
				            iv.setImageBitmap(bmp);
				            iv.setAdjustViewBounds(true);
				            l.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				            TextView txt = new TextView(AdminActivity.this);
				            txt.setText("IP : " + ipsLocales.replace(",", "  •  ") + "  •  port 8988\nLes participants scannent ce code depuis l'écran d'accueil.");
				            txt.setGravity(Gravity.CENTER);
				            txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				            txt.setPadding(0, 20, 0, 0);
				            l.addView(txt);
				            new AlertDialog.Builder(AdminActivity.this)
				                .setTitle("📱 Connexion des participants")
				                .setView(l)
				                .setPositiveButton("Fermer", null)
				                .show();
				        } catch (Exception e) {
				            Toast.makeText(AdminActivity.this, "Erreur QR : " + e.getMessage(), Toast.LENGTH_LONG).show();
				        }
				    }

				    // ---------- Consultation ----------
				    void afficherEcranConsultation() {
				        enConsultation = true;
				        screenContainer.removeAllViews();
				        screenContainer.setOrientation(LinearLayout.VERTICAL);

				        LinearLayout entete = new LinearLayout(AdminActivity.this);
				        entete.setOrientation(LinearLayout.VERTICAL);
				        entete.setPadding(30, 30, 30, 20);
				        entete.setBackgroundColor(Color.parseColor("#E8EAF6"));
				        TextView t1 = new TextView(AdminActivity.this);
				        t1.setText("🗂 Consultation");
				        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				        t1.setTypeface(null, Typeface.BOLD);
				        entete.addView(t1);
				        TextView t2 = new TextView(AdminActivity.this);
				        t2.setText(nomProjet[0] + " — scrutins terminés");
				        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
				        t2.setPadding(0, 0, 0, 10);
				        entete.addView(t2);
				        Button btnRetourC = new Button(AdminActivity.this);
				        btnRetourC.setText("⬅ Retour à l'écran principal");
				        btnRetourC.setOnClickListener(new View.OnClickListener() {
				            public void onClick(View v) {
				                afficherEcranPrincipal();
				            }
				        });
				        entete.addView(btnRetourC);
				        screenContainer.addView(entete);

				        ScrollView scroll = new ScrollView(AdminActivity.this);
				        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
				        scroll.setLayoutParams(lpScroll);
				        LinearLayout root = new LinearLayout(AdminActivity.this);
				        root.setOrientation(LinearLayout.VERTICAL);
				        root.setPadding(40, 30, 40, 80);
				        root.setBackgroundColor(Color.parseColor("#FFFFFF"));

				        int nb = 0;
				        for (int s = 0; s < sections.size(); s++) {
				            HashMap<String, Object> section = sections.get(s);
				            String titreSection = (String) section.get("titre");
				            ArrayList<HashMap<String, Object>> subs = (ArrayList<HashMap<String, Object>>) section.get("sousSections");
				            LinearLayout box = new LinearLayout(AdminActivity.this);
				            box.setOrientation(LinearLayout.VERTICAL);
				            box.setPadding(20, 20, 20, 20);
				            box.setBackgroundColor(Color.parseColor("#F0F0F0"));
				            LinearLayout.LayoutParams lpBox = new LinearLayout.LayoutParams(
				                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				            lpBox.setMargins(0, 0, 0, 30);
				            box.setLayoutParams(lpBox);
				            if (subs.size() == 0) {
				                if (!estResolu(section)) continue;
				                section.put("numLabel", romain(s + 1) + ". ");
				                afficherBlocVotable(box, section, true, sections, true, titreSection);
				                nb++;
				                root.addView(box);
				            } else {
				                int visibles = 0;
				                TextView titreS = new TextView(AdminActivity.this);
				                int termines = 0;
for (int k = 0; k < subs.size(); k++) if (estResolu(subs.get(k))) termines++;
titreS.setText(romain(s + 1) + ".  " + titreSection + (termines == subs.size() ? "   ✔ Section clôturée" : "   (" + termines + "/" + subs.size() + " terminés)"));
				                titreS.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				                titreS.setTypeface(null, Typeface.BOLD);
				                titreS.setTextColor(Color.parseColor("#283593"));
				                titreS.setPadding(0, 0, 0, 10);
				                box.addView(titreS);
				                for (int ss = 0; ss < subs.size(); ss++) {
				                    HashMap<String, Object> sub = subs.get(ss);
				                    if (!estResolu(sub)) continue;
				                    visibles++;
				                    sub.put("numLabel", (ss + 1) + ". ");
				                    LinearLayout sousBox = new LinearLayout(AdminActivity.this);
				                    sousBox.setOrientation(LinearLayout.VERTICAL);
				                    sousBox.setPadding(30, 15, 10, 15);
				                    afficherBlocVotable(sousBox, sub, false, subs, true, (String) sub.get("titre") + " " + titreSection);
				                    box.addView(sousBox);
				                }
				                if (visibles > 0) {
    nb++;
    if (sousSectionOuverte[0] == null) ajouterBoutonSousSection(box, section, subs);
    Button btnSupprC = new Button(AdminActivity.this);
    btnSupprC.setText("🗑 Supprimer la section");
    btnSupprC.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    final HashMap<String, Object> sectionC = section;
    btnSupprC.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            confirmerSuppressionSection(sectionC);
        }
    });
    box.addView(btnSupprC);
    root.addView(box);
}
				            }
				        }
				        if (nb == 0) {
				            TextView vide = new TextView(AdminActivity.this);
				            vide.setText("Aucun scrutin terminé pour le moment.");
				            vide.setTextColor(Color.parseColor("#888888"));
				            root.addView(vide);
				        }

				        Button btnTous = new Button(AdminActivity.this);
				        btnTous.setText("📊 Voir tous les résultats (élus)");
				        root.addView(btnTous);
				        btnTous.setOnClickListener(new View.OnClickListener() {
				            public void onClick(View v) {
				                afficherEcranResultats();
				            }
				        });
				        Button btnPdfC = new Button(AdminActivity.this);
				        btnPdfC.setText("📄 Exporter en PDF");
				        root.addView(btnPdfC);
				        btnPdfC.setOnClickListener(new View.OnClickListener() {
				            public void onClick(View v) {
				                exporterPdf();
				            }
				        });
				        scroll.addView(root);
				        screenContainer.addView(scroll);
				    }

				    // ---------- Export PDF ----------
				    void ajouterNomsPdf(ArrayList<Object[]> lignes, HashMap<String, Object> unite) {
				        String st = (String) unite.get("statut");
				        if ("designe".equals(st)) {
				            ArrayList<String> d = (ArrayList<String>) unite.get("designes");
				            if (d != null) for (int i = 0; i < d.size(); i++) lignes.add(new Object[]{"N", d.get(i), Boolean.FALSE});
				            return;
				        }
				        if (Boolean.TRUE.equals(unite.get("modeRoles"))) {
				            ArrayList<Object[]> aff = resoudreRoles(unite);
				            if (aff == null) return;
				            ArrayList<HashMap<String, Object>> roles = (ArrayList<HashMap<String, Object>>) unite.get("roles");
				            for (int r = 0; r < roles.size(); r++) {
				                String nomRole = (String) roles.get(r).get("nom");
				                boolean entete = false;
				                for (int i = 0; i < aff.size(); i++) {
				                    if (((String) aff.get(i)[2]).equals(nomRole)) {
				                        if (!entete) { lignes.add(new Object[]{"R", nomRole}); entete = true; }
				                        lignes.add(new Object[]{"N", (String) aff.get(i)[0], Boolean.TRUE});
				                    }
				                }
				            }
				            return;
				        }
				        ArrayList<Object[]> elus = calculerElus(unite);
				        if (elus == null) return;
				        for (int i = 0; i < elus.size(); i++) lignes.add(new Object[]{"N", (String) elus.get(i)[0], Boolean.FALSE});
				    }

				    ArrayList<Object[]> construireLignesPdf() {
				        ArrayList<Object[]> lignes = new ArrayList<Object[]>();
				        for (int s = 0; s < sections.size(); s++) {
				            HashMap<String, Object> section = sections.get(s);
				            ArrayList<HashMap<String, Object>> subs = (ArrayList<HashMap<String, Object>>) section.get("sousSections");
				            String titre = (String) section.get("titre");
				            if (subs.size() == 0) {
				                if (!estResolu(section)) continue;
				                lignes.add(new Object[]{"S", romain(s + 1), titre});
				                ajouterNomsPdf(lignes, section);
				            } else {
				                boolean unResolu = false;
				                for (int ss = 0; ss < subs.size(); ss++) if (estResolu(subs.get(ss))) unResolu = true;
				                if (!unResolu) continue;
				                lignes.add(new Object[]{"S", romain(s + 1), titre});
				                for (int ss = 0; ss < subs.size(); ss++) {
				                    HashMap<String, Object> sub = subs.get(ss);
				                    if (!estResolu(sub)) continue;
				                    lignes.add(new Object[]{"U", String.valueOf(ss + 1), (String) sub.get("titre")});
				                    ajouterNomsPdf(lignes, sub);
				                }
				            }
				        }
				        return lignes;
				    }

				    class PdfCtx {
				        PdfDocument doc;
				        PdfDocument.Page page;
				        Canvas c;
				        int num = 0;
				        int total;
				        float y = 0;
				        Paint pFond = new Paint(Paint.ANTI_ALIAS_FLAG);
				        Paint pTxt = new Paint(Paint.ANTI_ALIAS_FLAG);

				        PdfCtx(PdfDocument d, int t) {
				            doc = d;
				            total = t;
				        }

				        Paint fond(int couleur) {
				            pFond.setStyle(Paint.Style.FILL);
				            pFond.setColor(couleur);
				            return pFond;
				        }

				        Paint txt(int couleur, float taille, boolean gras, Paint.Align al) {
				            pTxt.setColor(couleur);
				            pTxt.setTextSize(taille);
				            pTxt.setTypeface(gras ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
				            pTxt.setTextAlign(al);
				            return pTxt;
				        }

				        float tracerTexte(Canvas cv, String t, float x, float yy, float maxW, Paint p, float lh) {
				            String reste = t.trim();
				            if (reste.length() == 0) return yy + lh;
				            while (reste.length() > 0) {
				                int n = p.breakText(reste, true, maxW, null);
				                if (n <= 0) n = 1;
				                if (n < reste.length()) {
				                    int idx = reste.lastIndexOf(' ', n);
				                    if (idx > 0) n = idx;
				                }
				                String ligne = reste.substring(0, n).trim();
				                if (cv != null) cv.drawText(ligne, x, yy, p);
				                yy += lh;
				                reste = reste.substring(n).trim();
				            }
				            return yy;
				        }

				        void bandeau() {
				            c.drawRect(0, 0, 595, 100, fond(0xFF1A237E));
				            c.drawRect(0, 100, 595, 105, fond(0xFF5C6BC0));
				            c.drawText("PROCÈS-VERBAL DES RÉSULTATS", 40, 34, txt(0xFFC5CAE9, 11, true, Paint.Align.LEFT));
				            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(new Date());
				            c.drawText(date, 555, 34, txt(0xFFC5CAE9, 11, false, Paint.Align.RIGHT));
				            tracerTexte(c, nomProjet[0], 40, 66, 515, txt(Color.WHITE, 22, true, Paint.Align.LEFT), 26);
				        }

				        void petitEntete() {
				            c.drawText(nomProjet[0], 40, 38, txt(0xFF7986CB, 10, true, Paint.Align.LEFT));
				            c.drawRect(40, 44, 555, 45.5f, fond(0xFFC5CAE9));
				        }

				        void pied() {
				            c.drawRect(40, 806, 555, 807, fond(0xFFC5CAE9));
				            c.drawText("VoteMGR", 40, 824, txt(0xFF9E9E9E, 9, false, Paint.Align.LEFT));
				            c.drawText("Page " + num + (total > 0 ? " / " + total : ""), 555, 824, txt(0xFF9E9E9E, 9, false, Paint.Align.RIGHT));
				        }

				        void nouvellePage() {
				            if (page != null) {
				                pied();
				                doc.finishPage(page);
				                page = null;
				            }
				            num++;
				            if (doc != null) {
				                page = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, num).create());
				                c = page.getCanvas();
				                if (num == 1) bandeau(); else petitEntete();
				            }
				            y = (num == 1) ? 135 : 70;
				        }

				        void assurer(float h) {
				            if (y + h > 790) nouvellePage();
				        }

				        void section(String rom, String titre) {
				            float w = Math.max(34, txt(Color.WHITE, 14, true, Paint.Align.CENTER).measureText(rom) + 20);
				            float maxW = 515 - w - 12;
				            float fin = tracerTexte(null, titre, 0, 0, maxW, txt(0xFF1A237E, 17, true, Paint.Align.LEFT), 21);
				            float h = Math.max(30, fin - 21 + 8);
				            assurer(h + 50);
				            y += 14;
				            if (c != null) {
				                c.drawRoundRect(new RectF(40, y, 40 + w, y + 30), 7, 7, fond(0xFF283593));
				                c.drawText(rom, 40 + w / 2, y + 21, txt(Color.WHITE, 14, true, Paint.Align.CENTER));
				            }
				            float yFin = tracerTexte(c, titre, 40 + w + 12, y + 21, maxW, txt(0xFF1A237E, 17, true, Paint.Align.LEFT), 21);
				            float bas = Math.max(y + 30, yFin - 21 + 8);
				            if (c != null) c.drawRect(40, bas + 4, 555, bas + 5.5f, fond(0xFF9FA8DA));
				            y = bas + 20;
				        }

				        void sous(String n, String titre) {
				            float maxW = 555 - 84;
				            float fin = tracerTexte(null, titre, 0, 0, maxW, txt(0xFF00695C, 13.5f, true, Paint.Align.LEFT), 17);
				            float h = Math.max(24, fin - 17 + 8);
				            assurer(h + 34);
				            y += 6;
				            if (c != null) {
				                c.drawCircle(62, y + 11, 11, fond(0xFF00897B));
				                c.drawText(n, 62, y + 15, txt(Color.WHITE, 11, true, Paint.Align.CENTER));
				            }
				            float yFin = tracerTexte(c, titre, 84, y + 15, maxW, txt(0xFF00695C, 13.5f, true, Paint.Align.LEFT), 17);
				            y = Math.max(y + 28, yFin - 17 + 14);
				        }

				        void role(String label) {
				            assurer(26);
				            if (c != null) c.drawText(label, 84, y + 12, txt(0xFF455A64, 11.5f, true, Paint.Align.LEFT));
				            y += 19;
				        }

				        void nom(String nomComplet, boolean avecRole) {
				            float x = avecRole ? 106 : 90;
				            assurer(24);
				            if (c != null) c.drawCircle(x - 10, y + 8, 2.5f, fond(0xFF00897B));
				            float yFin = tracerTexte(c, nomComplet, x, y + 12, 555 - x, txt(0xFF212121, 12.5f, false, Paint.Align.LEFT), 16);
				            y = yFin - 8;
				        }

				        void signatures() {
				            assurer(130);
				            y += 30;
				            if (c != null) c.drawText("Fait à ______________________________, le ____ / ____ / ________", 40, y, txt(0xFF424242, 11, false, Paint.Align.LEFT));
				            y += 36;
				            if (c != null) {
				                c.drawText("Le Président", 40, y, txt(0xFF424242, 11, true, Paint.Align.LEFT));
				                c.drawText("Le Secrétaire", 330, y, txt(0xFF424242, 11, true, Paint.Align.LEFT));
				                c.drawRect(40, y + 62, 240, y + 63, fond(0xFF9E9E9E));
				                c.drawRect(330, y + 62, 530, y + 63, fond(0xFF9E9E9E));
				            }
				            y += 70;
				        }

				        void fin() {
				            if (page != null) {
				                pied();
				                doc.finishPage(page);
				                page = null;
				            }
				        }
				    }

				    int rendrePdf(PdfDocument doc, ArrayList<Object[]> lignes, int total) {
				        PdfCtx p = new PdfCtx(doc, total);
				        p.nouvellePage();
				        for (int i = 0; i < lignes.size(); i++) {
				            Object[] l = lignes.get(i);
				            String type = (String) l[0];
				            if (type.equals("S")) p.section((String) l[1], (String) l[2]);
				            else if (type.equals("U")) p.sous((String) l[1], (String) l[2]);
				            else if (type.equals("R")) p.role((String) l[1]);
				            else p.nom((String) l[1], ((Boolean) l[2]).booleanValue());
				        }
				        p.signatures();
				        p.fin();
				        return p.num;
				    }

				    void exporterPdf() {
				        try {
				            ArrayList<Object[]> lignes = construireLignesPdf();
				            if (lignes.size() == 0) {
				                Toast.makeText(AdminActivity.this, "Aucun résultat clôturé à exporter", Toast.LENGTH_LONG).show();
				                return;
				            }
				            int total = rendrePdf(null, lignes, 0);
				            PdfDocument document = new PdfDocument();
				            rendrePdf(document, lignes, total);
				            ByteArrayOutputStream bos = new ByteArrayOutputStream();
				            document.writeTo(bos);
				            document.close();
				            byte[] data = bos.toByteArray();
				            String nomFichier = "resultats_" + nomProjet[0].replace(" ", "_") + ".pdf";
				            try {
				                File dossier = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
				                if (!dossier.exists()) dossier.mkdirs();
				                FileOutputStream fos = new FileOutputStream(new File(dossier, nomFichier));
				                fos.write(data);
				                fos.close();
				            } catch (Exception e2) {}
				            pdfEnAttente = data;
				            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				            intent.addCategory(Intent.CATEGORY_OPENABLE);
				            intent.setType("application/pdf");
				            intent.putExtra(Intent.EXTRA_TITLE, nomFichier);
				            startActivityForResult(intent, 502);
				        } catch (Exception e) {
				            Toast.makeText(AdminActivity.this, "Erreur export PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
				        }
				    }


				    void afficherCreationProjet() {
						        enConsultation = false;
						        screenContainer.removeAllViews();
						        LinearLayout layout = new LinearLayout(AdminActivity.this);
						        layout.setOrientation(LinearLayout.VERTICAL);
						        layout.setGravity(Gravity.CENTER_HORIZONTAL);
						        layout.setPadding(50, 150, 50, 50);
						        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView titre = new TextView(AdminActivity.this);
						        titre.setText("Nouveau projet de vote");
						        titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
						        titre.setPadding(0, 0, 0, 40);
						        layout.addView(titre);
						
						        final EditText champProjet = new EditText(AdminActivity.this);
						        champProjet.setHint("Nom du projet (ex: Assemblée 2026)");
						        layout.addView(champProjet);
						
						        Button btnCreer = new Button(AdminActivity.this);
						        btnCreer.setText("Créer le projet");
						        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lp.setMargins(0, 40, 0, 20);
						        btnCreer.setLayoutParams(lp);
						        layout.addView(btnCreer);
						
						        Button btnCharger = new Button(AdminActivity.this);
						        btnCharger.setText("📂 Charger un projet existant");
						        layout.addView(btnCharger);
						        btnCharger.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                listerEtChargerProjets();
										            }
								        });
						
						        btnCreer.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                String nom = champProjet.getText().toString().trim();
										                if (nom.isEmpty()) {
												                    Toast.makeText(AdminActivity.this, "Entrez un nom de projet", Toast.LENGTH_SHORT).show();
												                    return;
												                }
										                SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
										                prefs.edit().putString("membres_courant", "[]").apply();
										                nomProjet[0] = nom;
										                afficherEcranPrincipal();
										            }
								        });
						
						        screenContainer.addView(layout);
						    }
				
				    void afficherEcranPrincipal() {
						        enConsultation = false;
						        screenContainer.removeAllViews();
						        screenContainer.setOrientation(LinearLayout.VERTICAL);
						
						        LinearLayout barreFixe = new LinearLayout(AdminActivity.this);
						        barreFixe.setOrientation(LinearLayout.HORIZONTAL);
						        barreFixe.setPadding(20, 25, 20, 15);
						        
						
						        TextView titreBarre = new TextView(AdminActivity.this);
						        titreBarre.setText(nomProjet[0]);
						        titreBarre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
						        titreBarre.setTypeface(null, Typeface.BOLD);
						        LinearLayout.LayoutParams lpTitreBarre = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
						        titreBarre.setLayoutParams(lpTitreBarre);
						        barreFixe.addView(titreBarre);
						
						        btnVoterSticky = new Button(AdminActivity.this);
						        btnVoterSticky.setText("Voter");
						        barreFixe.addView(btnVoterSticky);
						        btnVoterSticky.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                if (sousSectionOuverte[0] == null) {
												                    Toast.makeText(AdminActivity.this, "Aucun vote ouvert actuellement", Toast.LENGTH_SHORT).show();
												                    return;
												                }
										                ArrayList<String> votantsActuels = (ArrayList<String>) sousSectionOuverte[0].get("votants");
										                if (votantsActuels.contains(nomAdmin)) {
												                    Toast.makeText(AdminActivity.this, "Vous avez déjà voté pour ce scrutin", Toast.LENGTH_SHORT).show();
												                    return;
												                }
										                afficherEcranVoteAdmin();
										            }
								        });
						
						        final LinearLayout enteteBox = new LinearLayout(AdminActivity.this);
					        enteteBox.setOrientation(LinearLayout.VERTICAL);
					        enteteBox.setBackgroundColor(Color.parseColor("#E8EAF6"));
					        enteteBox.addView(barreFixe);
					        screenContainer.addView(enteteBox);
						
						        LinearLayout barreAjout = new LinearLayout(AdminActivity.this);
						        barreAjout.setOrientation(LinearLayout.VERTICAL);
						        barreAjout.setPadding(20, 0, 20, 15);
						        
						        Button btnAjouterSectionSticky = new Button(AdminActivity.this);
						        btnAjouterSectionSticky.setText("➕\nSection");
						        final HorizontalScrollView defileOutils = new HorizontalScrollView(AdminActivity.this);
					        defileOutils.setHorizontalScrollBarEnabled(false);
					        final LinearLayout ligneOutils = new LinearLayout(AdminActivity.this);
					        ligneOutils.setOrientation(LinearLayout.HORIZONTAL);
					        defileOutils.addView(ligneOutils);
					        barreAjout.addView(defileOutils);
						        enteteBox.addView(barreAjout);
						
						        scrollPrincipal = new ScrollView(AdminActivity.this);
						        LinearLayout rootLayout = new LinearLayout(AdminActivity.this);
						        rootLayout.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.setPadding(40, 20, 40, 80);
						        rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView labelIp = new TextView(AdminActivity.this);
						        labelIp.setText("IP serveur : " + ipsLocales.replace(",", "  •  ") + "   ▸ toucher pour le QR");
labelIp.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        afficherQrConnexion();
    }
});
						        labelIp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
						        labelIp.setTextColor(Color.parseColor("#0066CC"));
						        labelIp.setPadding(0, 0, 0, 20);
						        rootLayout.addView(labelIp);
						
						        Button btnSauvegarder = new Button(AdminActivity.this);
						        btnSauvegarder.setText("💾\nSauver");
						        LinearLayout.LayoutParams lpSauv = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpSauv.setMargins(0, 0, 0, 10);
						        btnSauvegarder.setLayoutParams(lpSauv);
						        
						        btnSauvegarder.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                sauvegarderProjetFichier(true);
										            }
								        });
						
						        Button btnChargerAutre = new Button(AdminActivity.this);
						        btnChargerAutre.setText("📂\nCharger");
						        LinearLayout.LayoutParams lpChargerAutre = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpChargerAutre.setMargins(0, 0, 0, 10);
						        btnChargerAutre.setLayoutParams(lpChargerAutre);
						        
						        btnChargerAutre.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                listerEtChargerProjets();
										            }
								        });
						
						        Button btnSessions = new Button(AdminActivity.this);
						        btnSessions.setText("🗓\nSessions");
						        LinearLayout.LayoutParams lpSessions = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpSessions.setMargins(0, 0, 0, 10);
						        btnSessions.setLayoutParams(lpSessions);
						        
						        btnSessions.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranSessions();
										            }
								        });
						
						        Button btnListeMembres = new Button(AdminActivity.this);
						        btnListeMembres.setText("📜\nMembres");
						        LinearLayout.LayoutParams lpListeMembres = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpListeMembres.setMargins(0, 0, 0, 20);
						        btnListeMembres.setLayoutParams(lpListeMembres);
						        
						        btnListeMembres.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranListeMembres();
										            }
								        });
						
						        Button btnImporterListe = new Button(AdminActivity.this);
						        btnImporterListe.setText("📥\nImporter");
						        LinearLayout.LayoutParams lpBtnImport = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpBtnImport.setMargins(0, 0, 0, 20);
						        btnImporterListe.setLayoutParams(lpBtnImport);
						        
						        btnImporterListe.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
										                intent.setType("text/plain");
										                intent.addCategory(Intent.CATEGORY_OPENABLE);
										                startActivityForResult(intent, 501);
										            }
								        });
						
						        Button btnResultats = new Button(AdminActivity.this);
						        btnResultats.setText("🗂\nConsultation");
						        LinearLayout.LayoutParams lpBtnRes = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpBtnRes.setMargins(0, 0, 0, 20);
						        btnResultats.setLayoutParams(lpBtnRes);
						        final Button btnPdfBarre = new Button(AdminActivity.this);
					        btnPdfBarre.setText("📄\nExport PDF");
					        btnPdfBarre.setOnClickListener(new View.OnClickListener() {
							            public void onClick(View v) {
								                exporterPdf();
							            }
						        });
					        final Button btnQrBarre = new Button(AdminActivity.this);
					        btnQrBarre.setText("📱\nQR / IP");
					        btnQrBarre.setOnClickListener(new View.OnClickListener() {
							            public void onClick(View v) {
								                afficherQrConnexion();
							            }
						        });
					        Button[] ordreOutils = new Button[]{btnAjouterSectionSticky, btnQrBarre, btnImporterListe, btnSauvegarder, btnResultats, btnPdfBarre, btnSessions, btnListeMembres, btnChargerAutre};
					        for (int io = 0; io < ordreOutils.length; io++) {
							            ordreOutils[io].setTag("tool");
							            ordreOutils[io].setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
							            LinearLayout.LayoutParams lpOutil = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
							            lpOutil.setMargins(0, 0, 10, 0);
							            ligneOutils.addView(ordreOutils[io], lpOutil);
					        }
						        btnResultats.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranConsultation();
										            }
								        });
						
						        containerActif = new LinearLayout(AdminActivity.this);
						        containerActif.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.addView(containerActif);
						
						        scrollPrincipal.addView(rootLayout);
						        screenContainer.addView(scrollPrincipal);
						
						        redessiner();
						
						        btnAjouterSectionSticky.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                final EditText inputSection = new EditText(AdminActivity.this);
										                inputSection.setHint("Titre de la section (ex: Bureau exécutif)");
										                new AlertDialog.Builder(AdminActivity.this)
										                    .setTitle("Nouvelle section")
										                    .setView(inputSection)
										                    .setPositiveButton("Ajouter", new DialogInterface.OnClickListener() {
												                        public void onClick(DialogInterface dialog, int which) {
														                            String titreS = inputSection.getText().toString().trim();
														                            if (!titreS.isEmpty()) {
																                                HashMap<String, Object> section = new HashMap<String, Object>();
																                                section.put("titre", titreS);
																                                section.put("sousSections", new ArrayList<HashMap<String, Object>>());
																                                section.put("candidats", new ArrayList<String>());
																                                section.put("nbMax", 1);
																                                section.put("nbAdmis", 1);
																                                section.put("statut", "attente");
																                                section.put("votes", new HashMap<String, Integer>());
																                                section.put("votants", new ArrayList<String>());
																                                section.put("modeRoles", false);
																                                section.put("roles", new ArrayList<HashMap<String, Object>>());
																                                sections.add(section);
																                                section.put("plie", Boolean.FALSE);
scrollVersBasDemande = true;
																                                redessiner();
																                            }
														                        }
												                    })
										                    .setNegativeButton("Annuler", null)
										                    .show();
										            }
								        });
						    }
				
				    void afficherEcranSessions() {
						        enConsultation = false;
						        screenContainer.removeAllViews();
						        ScrollView scroll = new ScrollView(AdminActivity.this);
						        LinearLayout rootLayout = new LinearLayout(AdminActivity.this);
						        rootLayout.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.setPadding(40, 80, 40, 80);
						        rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView titre = new TextView(AdminActivity.this);
						        titre.setText("Sessions de présence");
						        titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
						        titre.setPadding(0, 0, 0, 20);
						        rootLayout.addView(titre);
						
						        for (int i = 0; i < sessions.size(); i++) {
								            final int index = i;
								            HashMap<String, Object> session = sessions.get(i);
								            ArrayList<String> presents = (ArrayList<String>) session.get("presents");
								
								            LinearLayout box = new LinearLayout(AdminActivity.this);
								            box.setOrientation(LinearLayout.VERTICAL);
								            box.setPadding(20, 15, 20, 15);
								            box.setBackgroundColor(Color.parseColor("#F0F0F0"));
								            LinearLayout.LayoutParams lpBox = new LinearLayout.LayoutParams(
								                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
								            lpBox.setMargins(0, 0, 0, 15);
								            box.setLayoutParams(lpBox);
								
								            TextView label = new TextView(AdminActivity.this);
								            label.setText((String) session.get("label") + " (" + presents.size() + " présent(s))");
								            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
								            label.setTypeface(null, Typeface.BOLD);
								            box.addView(label);
								
								            for (int j = 0; j < presents.size(); j++) {
										                TextView t = new TextView(AdminActivity.this);
										                t.setText("   • " + presents.get(j));
										                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
										                box.addView(t);
										            }
								
								            Button btnSup = new Button(AdminActivity.this);
								            btnSup.setText("🗑 Supprimer cette session");
								            btnSup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnSup.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    sessions.remove(index);
												                    sauvegarderProjetFichier(false);
												                    afficherEcranSessions();
												                }
										            });
								            box.addView(btnSup);
								
								            rootLayout.addView(box);
								        }
						
						        Button btnNouvelle = new Button(AdminActivity.this);
						        btnNouvelle.setText("+ Nouvelle session");
						        LinearLayout.LayoutParams lpNouvelle = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpNouvelle.setMargins(0, 20, 0, 20);
						        btnNouvelle.setLayoutParams(lpNouvelle);
						        rootLayout.addView(btnNouvelle);
						        btnNouvelle.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
										                String json = prefs.getString("membres_courant", "[]");
										                final ArrayList<String> listeImportee = new ArrayList<String>();
										                try {
												                    JSONArray arr = new JSONArray(json);
												                    for (int i = 0; i < arr.length(); i++) listeImportee.add(arr.getString(i));
												                } catch (Exception e) {}
										
										                if (listeImportee.size() == 0) {
												                    Toast.makeText(AdminActivity.this, "Importez d'abord une liste de membres (TXT) depuis l'écran principal", Toast.LENGTH_LONG).show();
												                    return;
												                }
										
										                LinearLayout dialogLayout = new LinearLayout(AdminActivity.this);
										                dialogLayout.setOrientation(LinearLayout.VERTICAL);
										                dialogLayout.setPadding(30, 20, 30, 20);
										
										                final EditText inputLabel = new EditText(AdminActivity.this);
										                inputLabel.setHint("Ex: Jour 1 - Matin");
										                dialogLayout.addView(inputLabel);
										
										                final EditText champRecherche = new EditText(AdminActivity.this);
										                champRecherche.setHint("Rechercher un nom...");
										                dialogLayout.addView(champRecherche);
										
										                final boolean[] toutCoche = {false};
										                final ArrayList<CheckBox> casesListe = new ArrayList<CheckBox>();
										                final Button btnToutCocher = new Button(AdminActivity.this);
										                btnToutCocher.setText("Tout sélectionner");
										                dialogLayout.addView(btnToutCocher);
										
										                final LinearLayout listeCasesLayout = new LinearLayout(AdminActivity.this);
										                listeCasesLayout.setOrientation(LinearLayout.VERTICAL);
										                dialogLayout.addView(listeCasesLayout);
										
										                for (int i = 0; i < listeImportee.size(); i++) {
												                    CheckBox cb = new CheckBox(AdminActivity.this);
												                    cb.setText(listeImportee.get(i));
												                    casesListe.add(cb);
												                    listeCasesLayout.addView(cb);
												                }
										
										                champRecherche.addTextChangedListener(new TextWatcher() {
												                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
												                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
												                    public void afterTextChanged(Editable s) {
														                        String filtre = s.toString().toLowerCase().trim();
														                        for (int i = 0; i < casesListe.size(); i++) {
																                            String nomCb = casesListe.get(i).getText().toString().toLowerCase();
																                            casesListe.get(i).setVisibility(filtre.isEmpty() || nomCb.contains(filtre) ? View.VISIBLE : View.GONE);
																                        }
														                    }
												                });
										
										                btnToutCocher.setOnClickListener(new View.OnClickListener() {
												                    public void onClick(View v2) {
														                        toutCoche[0] = !toutCoche[0];
														                        for (int i = 0; i < casesListe.size(); i++) casesListe.get(i).setChecked(toutCoche[0]);
														                        btnToutCocher.setText(toutCoche[0] ? "Tout désélectionner" : "Tout sélectionner");
														                    }
												                });
										
										                ScrollView scrollDialog = new ScrollView(AdminActivity.this);
										                scrollDialog.addView(dialogLayout);
										
										                new AlertDialog.Builder(AdminActivity.this)
										                    .setTitle("Nouvelle session")
										                    .setView(scrollDialog)
										                    .setPositiveButton("Créer", new DialogInterface.OnClickListener() {
												                        public void onClick(DialogInterface dialog, int which) {
														                            String label = inputLabel.getText().toString().trim();
														                            if (label.isEmpty()) {
																                                Toast.makeText(AdminActivity.this, "Entrez un nom de session", Toast.LENGTH_SHORT).show();
																                                return;
																                            }
														                            ArrayList<String> presents = new ArrayList<String>();
														                            for (int i = 0; i < casesListe.size(); i++) {
																                                if (casesListe.get(i).isChecked()) presents.add(casesListe.get(i).getText().toString());
																                            }
														                            HashMap<String, Object> session = new HashMap<String, Object>();
														                            session.put("label", label);
														                            session.put("presents", presents);
														                            sessions.add(session);
														                            sauvegarderProjetFichier(false);
														                            afficherEcranSessions();
														                        }
												                    })
										                    .setNegativeButton("Annuler", null)
										                    .show();
										            }
								        });
						
						        Button btnRetour = new Button(AdminActivity.this);
						        btnRetour.setText("Retour");
						        rootLayout.addView(btnRetour);
						        btnRetour.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranPrincipal();
										            }
								        });
						
						        scroll.addView(rootLayout);
						        screenContainer.addView(scroll);
						    }
				
				    void afficherEcranListeMembres() {
						        enConsultation = false;
						        screenContainer.removeAllViews();
						        ScrollView scroll = new ScrollView(AdminActivity.this);
						        LinearLayout rootLayout = new LinearLayout(AdminActivity.this);
						        rootLayout.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.setPadding(40, 80, 40, 80);
						        rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView titre = new TextView(AdminActivity.this);
						        titre.setText("Liste des membres");
						        titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
						        titre.setPadding(0, 0, 0, 20);
						        rootLayout.addView(titre);
						
						        SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
						        ArrayList<String> membres = new ArrayList<String>();
						        try {
								            JSONArray arr = new JSONArray(prefs.getString("membres_courant", "[]"));
								            for (int i = 0; i < arr.length(); i++) membres.add(arr.getString(i));
								        } catch (Exception e) {}
						
						        HashMap<String, ArrayList<String>> elusGlobal = calculerElusGlobal();
						
						        if (membres.size() == 0) {
								            TextView vide = new TextView(AdminActivity.this);
								            vide.setText("Aucun membre importé pour le moment.");
								            vide.setTextColor(Color.parseColor("#888888"));
								            rootLayout.addView(vide);
								        } else {
								            for (int i = 0; i < membres.size(); i++) {
										                String nom = membres.get(i);
										                TextView ligne = new TextView(AdminActivity.this);
										                ligne.setPadding(0, 10, 0, 10);
										                if (elusGlobal.containsKey(nom)) {
												                    ligne.setText(nom + "  ⭐ (" + joinListe(elusGlobal.get(nom)) + ")");
												                    ligne.setTypeface(null, Typeface.BOLD);
												                    ligne.setTextColor(Color.parseColor("#E65100"));
												                } else {
												                    ligne.setText(nom);
												                    ligne.setTextColor(Color.parseColor("#333333"));
												                }
										                ligne.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
										                rootLayout.addView(ligne);
										            }
								        }
						
						        Button btnRetour = new Button(AdminActivity.this);
						        btnRetour.setText("Retour");
						        LinearLayout.LayoutParams lpRetour = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpRetour.setMargins(0, 20, 0, 0);
						        btnRetour.setLayoutParams(lpRetour);
						        rootLayout.addView(btnRetour);
						        btnRetour.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranPrincipal();
										            }
								        });
						
						        scroll.addView(rootLayout);
						        screenContainer.addView(scroll);
						    }
				
				    void afficherEcranVoteAdmin() {
						        final HashMap<String, Object> unite = sousSectionOuverte[0];
						        enConsultation = false;
						        screenContainer.removeAllViews();
						        screenContainer.setOrientation(LinearLayout.VERTICAL);
						
						        ScrollView scroll = new ScrollView(AdminActivity.this);
						        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
						        scroll.setLayoutParams(lpScroll);
						
						        LinearLayout rootLayout = new LinearLayout(AdminActivity.this);
						        rootLayout.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.setPadding(40, 60, 40, 30);
						        rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView titre = new TextView(AdminActivity.this);
						        titre.setText((String) unite.get("titre"));
						        titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
						        titre.setPadding(0, 0, 0, 10);
						        rootLayout.addView(titre);
						
						        final int nbMax = (Integer) unite.get("nbMax");
						        TextView sousTitre = new TextView(AdminActivity.this);
						        sousTitre.setText("Cochez jusqu'à " + nbMax + " candidat(s)");
						        sousTitre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
						        sousTitre.setTextColor(Color.parseColor("#666666"));
						        sousTitre.setPadding(0, 0, 0, 15);
						        rootLayout.addView(sousTitre);
						
						        final EditText champRecherche = new EditText(AdminActivity.this);
						        champRecherche.setHint("Rechercher un nom...");
						        rootLayout.addView(champRecherche);
						
						        final ArrayList<String> candidats = (ArrayList<String>) unite.get("candidats");
						        final HashMap<String, ArrayList<String>> elusGlobal = calculerElusGlobal();
						        final ArrayList<CheckBox> cases = new ArrayList<CheckBox>();
						        for (int c = 0; c < candidats.size(); c++) {
								            String nomCand = candidats.get(c);
								            final CheckBox cb = new CheckBox(AdminActivity.this);
								            String suffix = elusGlobal.containsKey(nomCand) ? "  ⭐ (" + joinListe(elusGlobal.get(nomCand)) + ")" : "";
								            cb.setText(nomCand + suffix);
								            if (elusGlobal.containsKey(nomCand)) cb.setTextColor(Color.parseColor("#E65100"));
								            cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
								            cb.setPadding(15, 12, 0, 12);
								            cases.add(cb);
								            rootLayout.addView(cb);
								        }
						        for (int c = 0; c < cases.size(); c++) {
								            final CheckBox cb = cases.get(c);
								            cb.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    int total = 0;
												                    for (int k = 0; k < cases.size(); k++) if (cases.get(k).isChecked()) total++;
												                    if (total > nbMax) {
														                        cb.setChecked(false);
														                        Toast.makeText(AdminActivity.this, "Maximum " + nbMax + " choix", Toast.LENGTH_SHORT).show();
														                    }
												                }
										            });
								        }
						        champRecherche.addTextChangedListener(new TextWatcher() {
								            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
								            public void onTextChanged(CharSequence s, int start, int before, int count) {}
								            public void afterTextChanged(Editable s) {
										                String filtre = s.toString().toLowerCase().trim();
										                for (int i = 0; i < cases.size(); i++) {
												                    String nomCb = candidats.get(i).toLowerCase();
												                    cases.get(i).setVisibility(filtre.isEmpty() || nomCb.contains(filtre) ? View.VISIBLE : View.GONE);
												                }
										            }
								        });
						
						        scroll.addView(rootLayout);
						        screenContainer.addView(scroll);
						
						        LinearLayout barreBas = new LinearLayout(AdminActivity.this);
						        barreBas.setOrientation(LinearLayout.VERTICAL);
						        barreBas.setPadding(30, 15, 30, 15);
						        barreBas.setBackgroundColor(Color.parseColor("#E8EAF6"));
						
						        Button btnValider = new Button(AdminActivity.this);
						        btnValider.setText("✅ Valider mon vote");
						        barreBas.addView(btnValider);
						        btnValider.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                ArrayList<String> choix = new ArrayList<String>();
										                for (int k = 0; k < cases.size(); k++) if (cases.get(k).isChecked()) choix.add(candidats.get(k));
										                if (choix.size() == 0) {
												                    Toast.makeText(AdminActivity.this, "Cochez au moins un candidat", Toast.LENGTH_SHORT).show();
												                    return;
												                }
										                traiterVote(nomAdmin, choix);
										                Toast.makeText(AdminActivity.this, "Vote enregistré", Toast.LENGTH_SHORT).show();
										                afficherEcranPrincipal();
										            }
								        });
						
						        Button btnAnnuler = new Button(AdminActivity.this);
						        btnAnnuler.setText("Retour");
						        barreBas.addView(btnAnnuler);
						        btnAnnuler.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranPrincipal();
										            }
								        });
						
						        screenContainer.addView(barreBas);
						    }
				
				    void afficherEcranRoles(final HashMap<String, Object> unite) {
						        if (!unite.containsKey("roles")) unite.put("roles", new ArrayList<HashMap<String, Object>>());
						        enConsultation = false;
						        screenContainer.removeAllViews();
						        ScrollView scroll = new ScrollView(AdminActivity.this);
						        LinearLayout rootLayout = new LinearLayout(AdminActivity.this);
						        rootLayout.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.setPadding(40, 80, 40, 80);
						        rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView titre = new TextView(AdminActivity.this);
						        titre.setText("Postes — " + (String) unite.get("titre"));
						        titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
						        titre.setPadding(0, 0, 0, 20);
						        rootLayout.addView(titre);
						
						        final ArrayList<HashMap<String, Object>> roles = (ArrayList<HashMap<String, Object>>) unite.get("roles");
						        final LinearLayout listeRoles = new LinearLayout(AdminActivity.this);
						        listeRoles.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.addView(listeRoles);
						
						        final Runnable[] rafraichir = new Runnable[1];
						        rafraichir[0] = new Runnable() {
								            public void run() {
										                listeRoles.removeAllViews();
										                int total = 0;
										                for (int i = 0; i < roles.size(); i++) {
												                    final int index = i;
												                    HashMap<String, Object> role = roles.get(i);
												                    total += (Integer) role.get("nombre");
												                    LinearLayout ligne = new LinearLayout(AdminActivity.this);
												                    ligne.setOrientation(LinearLayout.HORIZONTAL);
												                    ligne.setPadding(0, 10, 0, 10);
												
												                    TextView txt = new TextView(AdminActivity.this);
												                    txt.setText((String) role.get("nom") + " : " + role.get("nombre") + " place(s)");
												                    txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
												                    LinearLayout.LayoutParams lpTxt = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
												                    txt.setLayoutParams(lpTxt);
												                    ligne.addView(txt);
												
												                    Button btnSup = new Button(AdminActivity.this);
												                    btnSup.setText("✕");
												                    btnSup.setOnClickListener(new View.OnClickListener() {
														                        public void onClick(View v) {
																                            roles.remove(index);
																                            rafraichir[0].run();
																                        }
														                    });
												                    ligne.addView(btnSup);
												
												                    listeRoles.addView(ligne);
												                }
										                TextView totalTxt = new TextView(AdminActivity.this);
										                totalTxt.setText("Total places : " + total);
										                totalTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
										                totalTxt.setTextColor(Color.parseColor("#0066CC"));
										                totalTxt.setPadding(0, 10, 0, 20);
										                listeRoles.addView(totalTxt);
										            }
								        };
						        rafraichir[0].run();
						
						        Button btnAjouterRole = new Button(AdminActivity.this);
						        btnAjouterRole.setText("+ Ajouter un poste");
						        rootLayout.addView(btnAjouterRole);
						        btnAjouterRole.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                LinearLayout dialogLayout = new LinearLayout(AdminActivity.this);
										                dialogLayout.setOrientation(LinearLayout.VERTICAL);
										                dialogLayout.setPadding(40, 20, 40, 20);
										
										                final EditText inputNom = new EditText(AdminActivity.this);
										                inputNom.setHint("Nom du poste (ex: Filoha)");
										                dialogLayout.addView(inputNom);
										
										                final EditText inputNombre = new EditText(AdminActivity.this);
										                inputNombre.setHint("Nombre de places");
										                inputNombre.setInputType(InputType.TYPE_CLASS_NUMBER);
										                dialogLayout.addView(inputNombre);
										
										                new AlertDialog.Builder(AdminActivity.this)
										                    .setTitle("Nouveau poste")
										                    .setView(dialogLayout)
										                    .setPositiveButton("Ajouter", new DialogInterface.OnClickListener() {
												                        public void onClick(DialogInterface dialog, int which) {
														                            String nom = inputNom.getText().toString().trim();
														                            String nbTxt = inputNombre.getText().toString().trim();
														                            if (!nom.isEmpty() && !nbTxt.isEmpty()) {
																                                HashMap<String, Object> role = new HashMap<String, Object>();
																                                role.put("nom", nom);
																                                role.put("nombre", Integer.parseInt(nbTxt));
																                                roles.add(role);
																                                rafraichir[0].run();
																                            }
														                        }
												                    })
										                    .setNegativeButton("Annuler", null)
										                    .show();
										            }
								        });
						
						        Button btnValider = new Button(AdminActivity.this);
						        btnValider.setText("Valider les postes");
						        LinearLayout.LayoutParams lpValider = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpValider.setMargins(0, 30, 0, 20);
						        btnValider.setLayoutParams(lpValider);
						        rootLayout.addView(btnValider);
						        btnValider.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                if (roles.size() == 0) {
												                    Toast.makeText(AdminActivity.this, "Ajoutez au moins un poste", Toast.LENGTH_SHORT).show();
												                    return;
												                }
										                int total = 0;
										                for (int i = 0; i < roles.size(); i++) total += (Integer) roles.get(i).get("nombre");
										                unite.put("modeRoles", true);
										                unite.put("nbAdmis", total);
										                afficherEcranPrincipal();
										            }
								        });
						
						        Button btnDesactiver = new Button(AdminActivity.this);
						        btnDesactiver.setText("Désactiver le mode postes");
						        rootLayout.addView(btnDesactiver);
						        btnDesactiver.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                unite.put("modeRoles", false);
										                afficherEcranPrincipal();
										            }
								        });
						
						        Button btnRetour = new Button(AdminActivity.this);
						        btnRetour.setText("Retour sans valider");
						        rootLayout.addView(btnRetour);
						        btnRetour.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranPrincipal();
										            }
								        });
						
						        scroll.addView(rootLayout);
						        screenContainer.addView(scroll);
						    }
				
				    ArrayList<Object[]> calculerElus(HashMap<String, Object> unite) {
						        String statut = (String) unite.get("statut");
						        if (!statut.equals("ferme")) return null;
						        ArrayList<String> candidats = actifs(unite);
						        HashMap<String, Integer> votes = (HashMap<String, Integer>) unite.get("votes");
						        int nbAdmis = unite.containsKey("nbAdmis") ? (Integer) unite.get("nbAdmis") : (Integer) unite.get("nbMax");
						        if (nbAdmis > candidats.size()) nbAdmis = candidats.size();
						        if (candidats.size() == 0) return new ArrayList<Object[]>();
						
						        ArrayList<Object[]> tries = new ArrayList<Object[]>();
						        for (int i = 0; i < candidats.size(); i++) {
								            String nom = candidats.get(i);
								            int score = votes.containsKey(nom) ? votes.get(nom) : 0;
								            tries.add(new Object[]{nom, score});
								        }
						        for (int i = 0; i < tries.size(); i++) {
								            for (int j = i + 1; j < tries.size(); j++) {
										                int si = (Integer) tries.get(i)[1];
										                int sj = (Integer) tries.get(j)[1];
										                if (sj > si) { Object[] tmp = tries.get(i); tries.set(i, tries.get(j)); tries.set(j, tmp); }
										            }
								        }
						
						        if (nbAdmis <= 0) return new ArrayList<Object[]>();
						        int seuilScore = (Integer) tries.get(nbAdmis - 1)[1];
						        ArrayList<Object[]> clairementElus = new ArrayList<Object[]>();
						        ArrayList<Object[]> exAequo = new ArrayList<Object[]>();
						        for (int i = 0; i < tries.size(); i++) {
								            int score = (Integer) tries.get(i)[1];
								            if (score > seuilScore) clairementElus.add(tries.get(i));
								            else if (score == seuilScore) exAequo.add(tries.get(i));
								        }
						
						        int placesRestantes = nbAdmis - clairementElus.size();
						        if (placesRestantes <= 0) return clairementElus;
						
						        if (placesRestantes == exAequo.size()) {
								            ArrayList<Object[]> resultat = new ArrayList<Object[]>();
								            resultat.addAll(clairementElus);
								            resultat.addAll(exAequo);
								            return resultat;
								        }
						
						        if (unite.containsKey("tourSuivant")) {
								            ArrayList<Object[]> subElus = calculerElus((HashMap<String, Object>) unite.get("tourSuivant"));
								            if (subElus == null) return null;
								            ArrayList<Object[]> resultat = new ArrayList<Object[]>();
								            resultat.addAll(clairementElus);
								            resultat.addAll(subElus);
								            return resultat;
								        }
						        return null;
						    }
				
				    void detecterEgalite(HashMap<String, Object> unite) {
						        ArrayList<String> candidats = actifs(unite);
						        HashMap<String, Integer> votes = (HashMap<String, Integer>) unite.get("votes");
						        int nbAdmis = unite.containsKey("nbAdmis") ? (Integer) unite.get("nbAdmis") : (Integer) unite.get("nbMax");
						        if (nbAdmis > candidats.size()) nbAdmis = candidats.size();
						        if (nbAdmis <= 0 || candidats.size() == 0) return;
						
						        ArrayList<Object[]> tries = new ArrayList<Object[]>();
						        for (int i = 0; i < candidats.size(); i++) {
								            String nom = candidats.get(i);
								            int score = votes.containsKey(nom) ? votes.get(nom) : 0;
								            tries.add(new Object[]{nom, score});
								        }
						        for (int i = 0; i < tries.size(); i++) {
								            for (int j = i + 1; j < tries.size(); j++) {
										                int si = (Integer) tries.get(i)[1];
										                int sj = (Integer) tries.get(j)[1];
										                if (sj > si) { Object[] tmp = tries.get(i); tries.set(i, tries.get(j)); tries.set(j, tmp); }
										            }
								        }
						
						        int seuilScore = (Integer) tries.get(nbAdmis - 1)[1];
						        int clairementElusCount = 0;
						        ArrayList<String> exAequo = new ArrayList<String>();
						        for (int i = 0; i < tries.size(); i++) {
								            int score = (Integer) tries.get(i)[1];
								            if (score > seuilScore) clairementElusCount++;
								            else if (score == seuilScore) exAequo.add((String) tries.get(i)[0]);
								        }
						
						        int placesRestantes = nbAdmis - clairementElusCount;
						        if (placesRestantes > 0 && placesRestantes < exAequo.size() && !unite.containsKey("tourSuivant")) {
								            HashMap<String, Object> departage = new HashMap<String, Object>();
								            departage.put("titre", (String) unite.get("titre") + " — Départage (égalité)");
								            departage.put("candidats", exAequo);
								            departage.put("nbMax", placesRestantes);
								            departage.put("nbAdmis", placesRestantes);
								            departage.put("statut", "attente");
								            departage.put("votes", new HashMap<String, Integer>());
								            departage.put("votants", new ArrayList<String>());
								            unite.put("tourSuivant", departage);
								        }
						    }
				
				    ArrayList<Object[]> resoudreRoles(HashMap<String, Object> racine) {
						        ArrayList<String> candidatsOriginaux = actifs(racine);
						        HashMap<String, Integer> votesOriginaux = (HashMap<String, Integer>) racine.get("votes");
						        ArrayList<HashMap<String, Object>> roles = (ArrayList<HashMap<String, Object>>) racine.get("roles");
						
						        ArrayList<Object[]> pool = new ArrayList<Object[]>();
						        for (int i = 0; i < candidatsOriginaux.size(); i++) {
								            String nom = candidatsOriginaux.get(i);
								            int score = votesOriginaux.containsKey(nom) ? votesOriginaux.get(nom) : 0;
								            pool.add(new Object[]{nom, score});
								        }
						        for (int i = 0; i < pool.size(); i++) {
								            for (int j = i + 1; j < pool.size(); j++) {
										                int si = (Integer) pool.get(i)[1];
										                int sj = (Integer) pool.get(j)[1];
										                if (sj > si) { Object[] tmp = pool.get(i); pool.set(i, pool.get(j)); pool.set(j, tmp); }
										            }
								        }
						
						        ArrayList<Object[]> resultat = new ArrayList<Object[]>();
						        HashMap<String, Object> noeudCourant = racine;
						
						        for (int r = 0; r < roles.size(); r++) {
								            HashMap<String, Object> role = roles.get(r);
								            String nomRole = (String) role.get("nom");
								            int n = (Integer) role.get("nombre");
								            if (n > pool.size()) n = pool.size();
								            if (n <= 0) continue;
								
								            int seuil = (Integer) pool.get(n - 1)[1];
								            int countClairs = 0;
								            ArrayList<Object[]> exaequo = new ArrayList<Object[]>();
								            for (int i = 0; i < pool.size(); i++) {
										                int score = (Integer) pool.get(i)[1];
										                if (score > seuil) countClairs++;
										                else if (score == seuil) exaequo.add(pool.get(i));
										            }
								            int placesRestantes = n - countClairs;
								            int consommeJusqua = countClairs + exaequo.size();
								
								            ArrayList<Object[]> bandMembers = new ArrayList<Object[]>();
								            for (int i = 0; i < countClairs; i++) bandMembers.add(pool.get(i));
								
								            if (placesRestantes == exaequo.size()) {
										                bandMembers.addAll(exaequo);
										                for (int i = 0; i < bandMembers.size(); i++) {
												                    Object[] m = bandMembers.get(i);
												                    resultat.add(new Object[]{m[0], m[1], nomRole});
												                }
										                ArrayList<Object[]> nouveauPool = new ArrayList<Object[]>();
										                for (int i = consommeJusqua; i < pool.size(); i++) nouveauPool.add(pool.get(i));
										                pool = nouveauPool;
										            } else if (placesRestantes > 0 && placesRestantes < exaequo.size()) {
										                if (noeudCourant.containsKey("tourSuivant")) {
												                    HashMap<String, Object> departage = (HashMap<String, Object>) noeudCourant.get("tourSuivant");
												                    if (!((String) departage.get("statut")).equals("ferme")) return null;
												
												                    ArrayList<String> depCandidats = (ArrayList<String>) departage.get("candidats");
												                    HashMap<String, Integer> depVotes = (HashMap<String, Integer>) departage.get("votes");
												                    ArrayList<Object[]> depTries = new ArrayList<Object[]>();
												                    for (int i = 0; i < depCandidats.size(); i++) {
														                        String nom = depCandidats.get(i);
														                        int score = depVotes.containsKey(nom) ? depVotes.get(nom) : 0;
														                        depTries.add(new Object[]{nom, score});
														                    }
												                    for (int i = 0; i < depTries.size(); i++) {
														                        for (int j = i + 1; j < depTries.size(); j++) {
																                            int si = (Integer) depTries.get(i)[1];
																                            int sj = (Integer) depTries.get(j)[1];
																                            if (sj > si) { Object[] tmp = depTries.get(i); depTries.set(i, depTries.get(j)); depTries.set(j, tmp); }
																                        }
														                    }
												                    int placesDep = placesRestantes;
												                    if (placesDep > depTries.size()) placesDep = depTries.size();
												                    int seuilDep = (Integer) depTries.get(placesDep - 1)[1];
												                    int depCountClairs = 0;
												                    ArrayList<Object[]> depExaequo = new ArrayList<Object[]>();
												                    for (int i = 0; i < depTries.size(); i++) {
														                        int score = (Integer) depTries.get(i)[1];
														                        if (score > seuilDep) depCountClairs++;
														                        else if (score == seuilDep) depExaequo.add(depTries.get(i));
														                    }
												                    int depPlacesRestantes = placesDep - depCountClairs;
												                    if (depPlacesRestantes > 0 && depPlacesRestantes < depExaequo.size()) {
														                        if (!departage.containsKey("tourSuivant")) {
																                            ArrayList<String> nomsExaequo = new ArrayList<String>();
																                            for (int i = 0; i < depExaequo.size(); i++) nomsExaequo.add((String) depExaequo.get(i)[0]);
																                            HashMap<String, Object> sousDepartage = new HashMap<String, Object>();
																                            sousDepartage.put("titre", (String) departage.get("titre") + " — Départage supplémentaire");
																                            sousDepartage.put("candidats", nomsExaequo);
																                            sousDepartage.put("nbMax", depPlacesRestantes);
																                            sousDepartage.put("nbAdmis", depPlacesRestantes);
																                            sousDepartage.put("statut", "attente");
																                            sousDepartage.put("votes", new HashMap<String, Integer>());
																                            sousDepartage.put("votants", new ArrayList<String>());
																                            sousDepartage.put("racineRoles", racine);
																                            departage.put("tourSuivant", sousDepartage);
																                        }
														                        return null;
														                    }
												                    ArrayList<Object[]> depBandMembers = new ArrayList<Object[]>();
												                    for (int i = 0; i < depCountClairs; i++) depBandMembers.add(depTries.get(i));
												                    depBandMembers.addAll(depExaequo);
												
												                    bandMembers.addAll(depBandMembers);
												                    for (int i = 0; i < bandMembers.size(); i++) {
														                        Object[] m = bandMembers.get(i);
														                        resultat.add(new Object[]{m[0], m[1], nomRole});
														                    }
												
												                    ArrayList<String> nomsGagnants = new ArrayList<String>();
												                    for (int i = 0; i < depBandMembers.size(); i++) nomsGagnants.add((String) depBandMembers.get(i)[0]);
												                    ArrayList<Object[]> perdants = new ArrayList<Object[]>();
												                    for (int i = 0; i < exaequo.size(); i++) {
														                        String nom = (String) exaequo.get(i)[0];
														                        if (!nomsGagnants.contains(nom)) perdants.add(exaequo.get(i));
														                    }
												                    ArrayList<Object[]> nouveauPool = new ArrayList<Object[]>();
												                    nouveauPool.addAll(perdants);
												                    for (int i = consommeJusqua; i < pool.size(); i++) nouveauPool.add(pool.get(i));
												                    pool = nouveauPool;
												                    noeudCourant = departage;
												                } else {
												                    ArrayList<String> nomsExaequo = new ArrayList<String>();
												                    for (int i = 0; i < exaequo.size(); i++) nomsExaequo.add((String) exaequo.get(i)[0]);
												                    HashMap<String, Object> departage = new HashMap<String, Object>();
												                    departage.put("titre", (String) racine.get("titre") + " — Départage \"" + nomRole + "\"");
												                    departage.put("candidats", nomsExaequo);
												                    departage.put("nbMax", placesRestantes);
												                    departage.put("nbAdmis", placesRestantes);
												                    departage.put("statut", "attente");
												                    departage.put("votes", new HashMap<String, Integer>());
												                    departage.put("votants", new ArrayList<String>());
												                    departage.put("racineRoles", racine);
												                    noeudCourant.put("tourSuivant", departage);
												                    return null;
												                }
										            } else {
										                for (int i = 0; i < bandMembers.size(); i++) {
												                    Object[] m = bandMembers.get(i);
												                    resultat.add(new Object[]{m[0], m[1], nomRole});
												                }
										                ArrayList<Object[]> nouveauPool = new ArrayList<Object[]>();
										                for (int i = consommeJusqua; i < pool.size(); i++) nouveauPool.add(pool.get(i));
										                pool = nouveauPool;
										            }
								        }
						        return resultat;
						    }
				
				    void ajouterElu(HashMap<String, ArrayList<String>> map, String nom, String poste) {
						        if (!map.containsKey(nom)) map.put(nom, new ArrayList<String>());
						        ArrayList<String> postes = map.get(nom);
						        if (!postes.contains(poste)) postes.add(poste);
						    }
				
				    void collecterElusUnite(HashMap<String, Object> unite, HashMap<String, ArrayList<String>> map, String posteBase) {
						        String statut = (String) unite.get("statut");
						        if (statut.equals("designe")) {
								            ArrayList<String> designes = (ArrayList<String>) unite.get("designes");
								            for (int i = 0; i < designes.size(); i++) ajouterElu(map, designes.get(i), posteBase);
								        } else if (statut.equals("ferme")) {
								            if (Boolean.TRUE.equals(unite.get("modeRoles"))) {
										                ArrayList<Object[]> affectation = resoudreRoles(unite);
										                if (affectation != null) {
												                    for (int i = 0; i < affectation.size(); i++) {
														                        ajouterElu(map, (String) affectation.get(i)[0], (String) affectation.get(i)[2] + " " + posteBase);
														                    }
												                }
										            } else {
										                ArrayList<Object[]> elus = calculerElus(unite);
										                if (elus != null) {
												                    for (int i = 0; i < elus.size(); i++) ajouterElu(map, (String) elus.get(i)[0], posteBase);
												                }
										            }
								        }
						    }
				
				    HashMap<String, ArrayList<String>> calculerElusGlobal() {
						        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
						        for (int s = 0; s < sections.size(); s++) {
								            HashMap<String, Object> section = sections.get(s);
								            ArrayList<HashMap<String, Object>> ss = (ArrayList<HashMap<String, Object>>) section.get("sousSections");
								            String titreSection = (String) section.get("titre");
								            if (ss.size() == 0) {
										                collecterElusUnite(section, map, titreSection);
										            } else {
										                for (int j = 0; j < ss.size(); j++) {
												                    HashMap<String, Object> sub = ss.get(j);
												                    collecterElusUnite(sub, map, (String) sub.get("titre") + " " + titreSection);
												                }
										            }
								        }
						        return map;
						    }
				void afficherBlocVotable(final LinearLayout parentBox, final HashMap<String, Object> unite, boolean estUneSection, final ArrayList<HashMap<String, Object>> listeParente, final boolean estRacine, final String posteBase) {
						        if (!unite.containsKey("statut")) unite.put("statut", "attente");
						        if (!unite.containsKey("votes")) unite.put("votes", new HashMap<String, Integer>());
						        if (!unite.containsKey("votants")) unite.put("votants", new ArrayList<String>());
						        if (!unite.containsKey("nbAdmis")) unite.put("nbAdmis", unite.get("nbMax"));
						        if (!unite.containsKey("modeRoles")) unite.put("modeRoles", false);
						        if (!unite.containsKey("roles")) unite.put("roles", new ArrayList<HashMap<String, Object>>());
						        String statut = (String) unite.get("statut");
						
						        String badge = statut.equals("ouvert") ? "🟢 OUVERT" : statut.equals("ferme") ? "🔴 FERMÉ" : statut.equals("designe") ? "📌 DÉSIGNÉ" : "⚪ EN ATTENTE";
						        TextView titreUnite = new TextView(AdminActivity.this);
						        String prefixe = unite.containsKey("numLabel") ? (String) unite.get("numLabel") : (estUneSection ? "" : "   - ");
						        titreUnite.setText(prefixe + badge + "  " + (String) unite.get("titre") + " (max " + unite.get("nbMax") + " choix, " + unite.get("nbAdmis") + " admis)");
						        titreUnite.setTextSize(TypedValue.COMPLEX_UNIT_SP, estUneSection ? 16 : 15);
						        titreUnite.setTypeface(null, Typeface.BOLD);
						        titreUnite.setTextColor(Color.parseColor("#283593"));
						        titreUnite.setPadding(0, 0, 0, 6);
						        if (!unite.containsKey("sansTitre")) parentBox.addView(titreUnite);
						
						        final ArrayList<String> candidats = (ArrayList<String>) unite.get("candidats");
						        final HashMap<String, Integer> votes = (HashMap<String, Integer>) unite.get("votes");
						        final HashMap<String, ArrayList<String>> elusGlobalCourant = calculerElusGlobal();
						
						        if (statut.equals("designe") || statut.equals("ferme")) {
								            final LinearLayout detailsBox = new LinearLayout(AdminActivity.this);
								            detailsBox.setOrientation(LinearLayout.VERTICAL);
								            detailsBox.setVisibility(View.GONE);
								
								            final Button btnToggle = new Button(AdminActivity.this);
								            btnToggle.setText("▼ Voir les résultats");
								            btnToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnToggle.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    if (detailsBox.getVisibility() == View.GONE) {
														                        detailsBox.setVisibility(View.VISIBLE);
														                        btnToggle.setText("▲ Masquer les résultats");
														                    } else {
														                        detailsBox.setVisibility(View.GONE);
														                        btnToggle.setText("▼ Voir les résultats");
														                    }
												                }
										            });
								            parentBox.addView(btnToggle);
								
								            if (statut.equals("designe")) {
										                ArrayList<String> designes = (ArrayList<String>) unite.get("designes");
										                TextView entete = new TextView(AdminActivity.this);
										                entete.setText("      📌 Désigné(s) :");
										                entete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
										                entete.setTypeface(null, Typeface.BOLD);
										                entete.setTextColor(Color.parseColor("#1B5E20"));
										                detailsBox.addView(entete);
										                for (int i = 0; i < designes.size(); i++) {
												                    TextView t = new TextView(AdminActivity.this);
												                    t.setText("         ✓ " + designes.get(i));
												                    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
												                    t.setTypeface(null, Typeface.BOLD);
												                    t.setTextColor(Color.parseColor("#1B5E20"));
												                    detailsBox.addView(t);
												                }
										            } else if (Boolean.TRUE.equals(unite.get("modeRoles"))) {
										                ArrayList<Object[]> affectation = resoudreRoles(unite);
										                if (affectation == null) {
												                    TextView attenteTxt = new TextView(AdminActivity.this);
												                    attenteTxt.setText("      🎭 Départage de poste en cours (voir plus bas)");
												                    attenteTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
												                    attenteTxt.setTextColor(Color.parseColor("#CC6600"));
												                    detailsBox.addView(attenteTxt);
												                } else {
												                    ArrayList<HashMap<String, Object>> roles = (ArrayList<HashMap<String, Object>>) unite.get("roles");
												                    for (int r = 0; r < roles.size(); r++) {
														                        String nomRole = (String) roles.get(r).get("nom");
														                        TextView headerRole = new TextView(AdminActivity.this);
														                        headerRole.setText("      🏆 " + nomRole + " :");
														                        headerRole.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
														                        headerRole.setTypeface(null, Typeface.BOLD);
														                        headerRole.setTextColor(Color.parseColor("#006600"));
														                        detailsBox.addView(headerRole);
														                        for (int i = 0; i < affectation.size(); i++) {
																                            Object[] a = affectation.get(i);
																                            if (((String) a[2]).equals(nomRole)) {
																		                                TextView t = new TextView(AdminActivity.this);
																		                                t.setText("         ✓ " + (String) a[0]);
																		                                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
																		                                t.setTypeface(null, Typeface.BOLD);
																		                                t.setTextColor(Color.parseColor("#1B5E20"));
																		                                detailsBox.addView(t);
																		                            }
																                        }
														                    }
												                }
										            } else {
										                ArrayList<Object[]> elus = calculerElus(unite);
										                if (elus == null) {
												                    TextView attenteTxt = new TextView(AdminActivity.this);
												                    attenteTxt.setText("      ⚖️ Égalité détectée — départage en cours");
												                    attenteTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
												                    attenteTxt.setTextColor(Color.parseColor("#CC6600"));
												                    detailsBox.addView(attenteTxt);
												                } else {
												                    TextView entete = new TextView(AdminActivity.this);
												                    entete.setText("      🏆 Élus (" + elus.size() + "/" + unite.get("nbAdmis") + ") :");
												                    entete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
												                    entete.setTypeface(null, Typeface.BOLD);
												                    entete.setTextColor(Color.parseColor("#006600"));
												                    detailsBox.addView(entete);
												                    for (int i = 0; i < elus.size(); i++) {
														                        Object[] e = elus.get(i);
														                        TextView eluTxt = new TextView(AdminActivity.this);
														                        eluTxt.setText("         ✓ " + (String) e[0]);
														                        eluTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
														                        eluTxt.setTypeface(null, Typeface.BOLD);
														                        eluTxt.setTextColor(Color.parseColor("#1B5E20"));
														                        detailsBox.addView(eluTxt);
														                    }
												                }
										            }
								            if (statut.equals("ferme")) {
    ajouterBlocElimination(detailsBox, unite);
}
parentBox.addView(detailsBox);
								        } else {
								            final LinearLayout listeCandBox = new LinearLayout(AdminActivity.this);
listeCandBox.setOrientation(LinearLayout.VERTICAL);
if (statut.equals("attente") && candidats.size() > 0) {
    listeCandBox.setVisibility(View.GONE);
    final Button btnCandToggle = new Button(AdminActivity.this);
    btnCandToggle.setText("👥 " + candidats.size() + " candidat(s) ▼");
    btnCandToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    btnCandToggle.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (listeCandBox.getVisibility() == View.GONE) {
                listeCandBox.setVisibility(View.VISIBLE);
                btnCandToggle.setText("👥 " + candidats.size() + " candidat(s) ▲");
            } else {
                listeCandBox.setVisibility(View.GONE);
                btnCandToggle.setText("👥 " + candidats.size() + " candidat(s) ▼");
            }
        }
    });
    parentBox.addView(btnCandToggle);
}
parentBox.addView(listeCandBox);
for (int c = 0; c < candidats.size(); c++) {
										                String nomCand = candidats.get(c);
										                String suffixElu = elusGlobalCourant.containsKey(nomCand) ? "  ⭐ (" + joinListe(elusGlobalCourant.get(nomCand)) + ")" : "";
										                TextView candTxt = new TextView(AdminActivity.this);
										                int score = votes.containsKey(nomCand) ? votes.get(nomCand) : 0;
										                if (statut.equals("ouvert")) {
												                    candTxt.setText("      • " + nomCand + suffixElu + "  —  " + score + " voix");
												                } else {
												                    candTxt.setText("      • " + nomCand + suffixElu);
												                }
										                candTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
										                candTxt.setTextColor(Color.parseColor("#555555"));
										                listeCandBox.addView(candTxt);
										            }
								        }
						
						        if (statut.equals("attente")) {
								            final ArrayList<Button> optionsUnite = new ArrayList<Button>();
Button btnDepuisListe = new Button(AdminActivity.this);
								            btnDepuisListe.setText("📋 Liste");
								            btnDepuisListe.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnDepuisListe.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
												                    String json = prefs.getString("membres_courant", "[]");
												                    final ArrayList<String> listeImportee = new ArrayList<String>();
												                    try {
														                        JSONArray arr = new JSONArray(json);
														                        for (int i = 0; i < arr.length(); i++) listeImportee.add(arr.getString(i));
														                    } catch (Exception e) {}
												
												                    if (listeImportee.size() == 0) {
														                        Toast.makeText(AdminActivity.this, "Aucune liste importée. Importez un fichier TXT depuis l'écran principal.", Toast.LENGTH_LONG).show();
														                        return;
														                    }
												
												                    final HashMap<String, ArrayList<String>> elusGlobalImport = calculerElusGlobal();
												
												                    LinearLayout dialogLayout = new LinearLayout(AdminActivity.this);
												                    dialogLayout.setOrientation(LinearLayout.VERTICAL);
												                    dialogLayout.setPadding(30, 20, 30, 20);
												
												                    final EditText champRecherche = new EditText(AdminActivity.this);
												                    champRecherche.setHint("Rechercher un nom...");
												                    dialogLayout.addView(champRecherche);
												
												                    final boolean[] toutCoche = {false};
												                    final ArrayList<CheckBox> casesListe = new ArrayList<CheckBox>();
												                    final ArrayList<String> nomsBruts = new ArrayList<String>();
												
												                    final Button btnToutCocher = new Button(AdminActivity.this);
												                    btnToutCocher.setText("Tout sélectionner");
												                    dialogLayout.addView(btnToutCocher);
												
												                    final LinearLayout listeCasesLayout = new LinearLayout(AdminActivity.this);
												                    listeCasesLayout.setOrientation(LinearLayout.VERTICAL);
												                    dialogLayout.addView(listeCasesLayout);
												
												                    for (int i = 0; i < listeImportee.size(); i++) {
														                        String nomImp = listeImportee.get(i);
														                        nomsBruts.add(nomImp);
														                        CheckBox cb = new CheckBox(AdminActivity.this);
														                        String suffixElu = elusGlobalImport.containsKey(nomImp) ? "  ⭐ (" + joinListe(elusGlobalImport.get(nomImp)) + ")" : "";
														                        cb.setText(nomImp + suffixElu);
														                        if (elusGlobalImport.containsKey(nomImp)) {
																                            cb.setTextColor(Color.parseColor("#E65100"));
																                        }
														                        if (candidats.contains(nomImp)) {
																                            cb.setChecked(true);
																                            cb.setEnabled(false);
																                        }
														                        casesListe.add(cb);
														                        listeCasesLayout.addView(cb);
														                    }
												
												                    champRecherche.addTextChangedListener(new TextWatcher() {
														                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
														                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
														                        public void afterTextChanged(Editable s) {
																                            String filtre = s.toString().toLowerCase().trim();
																                            for (int i = 0; i < casesListe.size(); i++) {
																		                                String nomCb = nomsBruts.get(i).toLowerCase();
																		                                casesListe.get(i).setVisibility(filtre.isEmpty() || nomCb.contains(filtre) ? View.VISIBLE : View.GONE);
																		                            }
																                        }
														                    });
												
												                    btnToutCocher.setOnClickListener(new View.OnClickListener() {
														                        public void onClick(View v2) {
																                            toutCoche[0] = !toutCoche[0];
																                            for (int i = 0; i < casesListe.size(); i++) {
																		                                if (casesListe.get(i).isEnabled()) {
																				                                    casesListe.get(i).setChecked(toutCoche[0]);
																				                                }
																		                            }
																                            btnToutCocher.setText(toutCoche[0] ? "Tout désélectionner" : "Tout sélectionner");
																                        }
														                    });
												
												                    ScrollView scrollDialog = new ScrollView(AdminActivity.this);
												                    scrollDialog.addView(dialogLayout);
												
												                    new AlertDialog.Builder(AdminActivity.this)
												                        .setTitle("Sélectionner depuis la liste importée")
												                        .setView(scrollDialog)
												                        .setPositiveButton("Ajouter la sélection", new DialogInterface.OnClickListener() {
														                            public void onClick(DialogInterface dialog, int which) {
																                                int ajoutes = 0;
																                                for (int i = 0; i < casesListe.size(); i++) {
																		                                    if (casesListe.get(i).isChecked() && casesListe.get(i).isEnabled()) {
																				                                        String nom = nomsBruts.get(i);
																				                                        if (!candidats.contains(nom)) {
																						                                            candidats.add(nom);
																						                                            ajoutes++;
																						                                        }
																				                                    }
																		                                }
																                                Toast.makeText(AdminActivity.this, ajoutes + " candidat(s) ajouté(s)", Toast.LENGTH_SHORT).show();
																                                redessiner();
																                            }
														                        })
												                        .setNegativeButton("Annuler", null)
												                        .show();
												                }
										            });
								            LinearLayout rangeeCand = new LinearLayout(AdminActivity.this);
rangeeCand.setOrientation(LinearLayout.HORIZONTAL);
Button btnSaisie = new Button(AdminActivity.this);
btnSaisie.setText("✍ Saisir");
btnSaisie.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
btnSaisie.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        LinearLayout cadre = new LinearLayout(AdminActivity.this);
        cadre.setPadding(40, 20, 40, 10);
        final EditText saisie = new EditText(AdminActivity.this);
        saisie.setHint("Un nom par ligne");
        saisie.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        saisie.setMinLines(3);
        saisie.setGravity(Gravity.TOP | Gravity.START);
        cadre.addView(saisie, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(AdminActivity.this)
            .setTitle("Ajouter manuellement")
            .setView(cadre)
            .setPositiveButton("Ajouter", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String[] lignes = saisie.getText().toString().split("\\r?\\n");
                    int ajoutes = 0;
                    for (int i = 0; i < lignes.length; i++) {
                        String nomSaisi = lignes[i].trim();
                        if (!nomSaisi.isEmpty() && !candidats.contains(nomSaisi)) {
                            candidats.add(nomSaisi);
                            ajoutes++;
                        }
                    }
                    Toast.makeText(AdminActivity.this, ajoutes + " candidat(s) ajouté(s)", Toast.LENGTH_SHORT).show();
                    redessiner();
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
});
Button btnRetirer = new Button(AdminActivity.this);
btnRetirer.setText("🧹 Retirer");
btnRetirer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
btnRetirer.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        if (candidats.size() == 0) {
            Toast.makeText(AdminActivity.this, "Aucun candidat à retirer", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] nomsR = candidats.toArray(new String[0]);
        final boolean[] cochesR = new boolean[nomsR.length];
        new AlertDialog.Builder(AdminActivity.this)
            .setTitle("Retirer des candidats")
            .setMultiChoiceItems(nomsR, cochesR, new DialogInterface.OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    cochesR[which] = isChecked;
                }
            })
            .setPositiveButton("Retirer", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    for (int i = 0; i < nomsR.length; i++) {
                        if (cochesR[i]) candidats.remove(nomsR[i]);
                    }
                    redessiner();
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
});
Button[] boutonsCand = new Button[]{btnDepuisListe, btnSaisie, btnRetirer};
for (int ib = 0; ib < boutonsCand.length; ib++) {
    LinearLayout.LayoutParams lpCand = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    lpCand.setMargins(3, 0, 3, 0);
    rangeeCand.addView(boutonsCand[ib], lpCand);
}
parentBox.addView(rangeeCand);
								
								            Button btnNbMax = new Button(AdminActivity.this);
								            btnNbMax.setText("Nb choix: " + unite.get("nbMax"));
								            btnNbMax.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnNbMax.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    final EditText inputNb = new EditText(AdminActivity.this);
												                    inputNb.setInputType(InputType.TYPE_CLASS_NUMBER);
												                    inputNb.setHint("Nombre de candidats à cocher");
												                    new AlertDialog.Builder(AdminActivity.this)
												                        .setTitle("Nombre de choix autorisés")
												                        .setView(inputNb)
												                        .setPositiveButton("Valider", new DialogInterface.OnClickListener() {
														                            public void onClick(DialogInterface dialog, int which) {
																                                String txt = inputNb.getText().toString().trim();
																                                if (!txt.isEmpty()) {
																		                                    unite.put("nbMax", Integer.parseInt(txt));
																		                                    redessiner();
																		                                }
																                            }
														                        })
												                        .setNegativeButton("Annuler", null)
												                        .show();
												                }
										            });
								            optionsUnite.add(btnNbMax);
								
								            if (!Boolean.TRUE.equals(unite.get("modeRoles"))) {
										                Button btnNbAdmis = new Button(AdminActivity.this);
										                btnNbAdmis.setText("Nb admis: " + unite.get("nbAdmis"));
										                btnNbAdmis.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
										                btnNbAdmis.setOnClickListener(new View.OnClickListener() {
												                    public void onClick(View v) {
														                        final EditText inputNb = new EditText(AdminActivity.this);
														                        inputNb.setInputType(InputType.TYPE_CLASS_NUMBER);
														                        inputNb.setHint("Nombre de candidats admis (élus)");
														                        new AlertDialog.Builder(AdminActivity.this)
														                            .setTitle("Nombre de candidats admis")
														                            .setView(inputNb)
														                            .setPositiveButton("Valider", new DialogInterface.OnClickListener() {
																                                public void onClick(DialogInterface dialog, int which) {
																		                                    String txt = inputNb.getText().toString().trim();
																		                                    if (!txt.isEmpty()) {
																				                                        unite.put("nbAdmis", Integer.parseInt(txt));
																				                                        redessiner();
																				                                    }
																		                                }
																                            })
														                            .setNegativeButton("Annuler", null)
														                            .show();
														                    }
												                });
										                optionsUnite.add(btnNbAdmis);
										            }
								
								            Button btnRoles = new Button(AdminActivity.this);
								            btnRoles.setText(Boolean.TRUE.equals(unite.get("modeRoles")) ? "🎭 Modifier les postes" : "🎭 Configurer des postes");
								            btnRoles.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnRoles.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    afficherEcranRoles(unite);
												                }
										            });
								            optionsUnite.add(btnRoles);
								
								            Button btnDesigner = new Button(AdminActivity.this);
								            btnDesigner.setText("🏷 Désigner sans vote");
								            btnDesigner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnDesigner.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    if (candidats.size() == 0) {
														                        Toast.makeText(AdminActivity.this, "Ajoutez d'abord des candidats via la liste importée", Toast.LENGTH_SHORT).show();
														                        return;
														                    }
												                    final String[] noms = candidats.toArray(new String[0]);
												                    final boolean[] coches = new boolean[noms.length];
												                    final HashMap<String, ArrayList<String>> elusGlobalDesigner = calculerElusGlobal();
												
												                    LinearLayout dialogLayout = new LinearLayout(AdminActivity.this);
												                    dialogLayout.setOrientation(LinearLayout.VERTICAL);
												                    dialogLayout.setPadding(30, 20, 30, 20);
												
												                    final EditText champRecherche = new EditText(AdminActivity.this);
												                    champRecherche.setHint("Rechercher un nom...");
												                    dialogLayout.addView(champRecherche);
												
												                    final LinearLayout listeCasesLayout = new LinearLayout(AdminActivity.this);
												                    listeCasesLayout.setOrientation(LinearLayout.VERTICAL);
												                    dialogLayout.addView(listeCasesLayout);
												
												                    final ArrayList<CheckBox> casesDesigner = new ArrayList<CheckBox>();
												                    for (int i = 0; i < noms.length; i++) {
														                        final int index = i;
														                        CheckBox cb = new CheckBox(AdminActivity.this);
														                        String suffixElu = elusGlobalDesigner.containsKey(noms[i]) ? "  ⭐ (" + joinListe(elusGlobalDesigner.get(noms[i])) + ")" : "";
														                        cb.setText(noms[i] + suffixElu);
														                        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
																                            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
																		                                coches[index] = isChecked;
																		                            }
																                        });
														                        casesDesigner.add(cb);
														                        listeCasesLayout.addView(cb);
														                    }
												
												                    champRecherche.addTextChangedListener(new TextWatcher() {
														                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
														                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
														                        public void afterTextChanged(Editable s) {
																                            String filtre = s.toString().toLowerCase().trim();
																                            for (int i = 0; i < casesDesigner.size(); i++) {
																		                                String nomCb = noms[i].toLowerCase();
																		                                casesDesigner.get(i).setVisibility(filtre.isEmpty() || nomCb.contains(filtre) ? View.VISIBLE : View.GONE);
																		                            }
																                        }
														                    });
												
												                    ScrollView scrollDialog = new ScrollView(AdminActivity.this);
												                    scrollDialog.addView(dialogLayout);
												
												                    new AlertDialog.Builder(AdminActivity.this)
												                        .setTitle("Désigner directement")
												                        .setView(scrollDialog)
												                        .setPositiveButton("Valider", new DialogInterface.OnClickListener() {
														                            public void onClick(DialogInterface dialog, int which) {
																                                ArrayList<String> selection = new ArrayList<String>();
																                                for (int i = 0; i < noms.length; i++) if (coches[i]) selection.add(noms[i]);
																                                if (selection.size() == 0) {
																		                                    Toast.makeText(AdminActivity.this, "Sélectionnez au moins une personne", Toast.LENGTH_SHORT).show();
																		                                    return;
																		                                }
																                                unite.put("statut", "designe");
																                                unite.put("designes", selection);
																                                redessiner();
																                                broadcastEtat();
																                                Toast.makeText(AdminActivity.this, "Désignation enregistrée", Toast.LENGTH_SHORT).show();
																                            }
														                        })
												                        .setNegativeButton("Annuler", null)
												                        .show();
												                }
										            });
								            optionsUnite.add(btnDesigner);
								
								            Button btnOuvrir = new Button(AdminActivity.this);
								            btnOuvrir.setText("Ouvrir le vote");
								            btnOuvrir.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnOuvrir.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    if (sousSectionOuverte[0] != null) {
														                        Toast.makeText(AdminActivity.this, "Fermez d'abord le vote en cours", Toast.LENGTH_SHORT).show();
														                        return;
														                    }
												                    if (candidats.size() == 0) {
														                        Toast.makeText(AdminActivity.this, "Ajoutez au moins un candidat", Toast.LENGTH_SHORT).show();
														                        return;
														                    }
												                    int nbAdmisVal = (Integer) unite.get("nbAdmis");
												                    if (nbAdmisVal <= 0) {
														                        Toast.makeText(AdminActivity.this, "Nombre d'admis invalide", Toast.LENGTH_SHORT).show();
														                        return;
														                    }
												                    if (nbAdmisVal > candidats.size()) {
														                        Toast.makeText(AdminActivity.this, "Le nombre d'admis dépasse le nombre de candidats", Toast.LENGTH_SHORT).show();
														                        return;
														                    }
												                    unite.put("statut", "ouvert");
												                    unite.put("votants", new ArrayList<String>());
												                    unite.put("votes", new HashMap<String, Integer>());
												                    sousSectionOuverte[0] = unite;
												                    redessiner();
												                    broadcastEtat();
												                    Toast.makeText(AdminActivity.this, "Vote ouvert et envoyé aux participants", Toast.LENGTH_SHORT).show();
												                }
										            });
								            Button btnOptions = new Button(AdminActivity.this);
btnOptions.setText("⚙ Options du scrutin");
btnOptions.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
btnOptions.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        final CharSequence[] etiquettes = new CharSequence[optionsUnite.size()];
        for (int io = 0; io < optionsUnite.size(); io++) etiquettes[io] = optionsUnite.get(io).getText();
        new AlertDialog.Builder(AdminActivity.this)
            .setTitle("Options du scrutin")
            .setItems(etiquettes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    optionsUnite.get(which).performClick();
                }
            })
            .setNegativeButton("Fermer", null)
            .show();
    }
});
parentBox.addView(btnOptions);
parentBox.addView(btnOuvrir);
								        } else if (statut.equals("ouvert")) {
								            Button btnFermer = new Button(AdminActivity.this);
								            btnFermer.setText("Clôturer le vote");
								            btnFermer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
								            btnFermer.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    unite.put("statut", "ferme");
												                    sousSectionOuverte[0] = null;
												                    if (Boolean.TRUE.equals(unite.get("modeRoles"))) {
														                        resoudreRoles(unite);
														                    } else if (unite.containsKey("racineRoles")) {
														                        resoudreRoles((HashMap<String, Object>) unite.get("racineRoles"));
														                    } else {
														                        detecterEgalite(unite);
														                    }
												                    redessiner();
												                    broadcastEtat();
												                    Toast.makeText(AdminActivity.this, "Vote clôturé", Toast.LENGTH_SHORT).show();
												                }
										            });
								            parentBox.addView(btnFermer);
								        } else if (statut.equals("ferme") || statut.equals("designe")) {
								            if (statut.equals("ferme") && !Boolean.TRUE.equals(unite.get("modeRoles")) && !unite.containsKey("tourSuivant") && sousSectionOuverte[0] == null) {
										                Button btnSecondTour = new Button(AdminActivity.this);
										                btnSecondTour.setText("Créer un second tour (ex-æquo)");
										                btnSecondTour.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
										                btnSecondTour.setOnClickListener(new View.OnClickListener() {
												                    public void onClick(View v) {
														                        final String[] nomsCandidats = candidats.toArray(new String[0]);
														                        final boolean[] coches = new boolean[nomsCandidats.length];
														                        new AlertDialog.Builder(AdminActivity.this)
														                            .setTitle("Candidats du second tour")
														                            .setMultiChoiceItems(nomsCandidats, coches, new DialogInterface.OnMultiChoiceClickListener() {
																                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
																		                                    coches[which] = isChecked;
																		                                }
																                            })
														                            .setPositiveButton("Créer", new DialogInterface.OnClickListener() {
																                                public void onClick(DialogInterface dialog, int which) {
																		                                    ArrayList<String> selection = new ArrayList<String>();
																		                                    for (int i = 0; i < nomsCandidats.length; i++) {
																				                                        if (coches[i]) selection.add(nomsCandidats[i]);
																				                                    }
																		                                    if (selection.size() < 2) {
																				                                        Toast.makeText(AdminActivity.this, "Sélectionnez au moins 2 candidats", Toast.LENGTH_SHORT).show();
																				                                        return;
																				                                    }
																		                                    HashMap<String, Object> nouveauTour = new HashMap<String, Object>();
																		                                    nouveauTour.put("titre", (String) unite.get("titre") + " (2e tour)");
																		                                    nouveauTour.put("candidats", selection);
																		                                    nouveauTour.put("nbMax", unite.get("nbMax"));
																		                                    nouveauTour.put("nbAdmis", unite.get("nbMax"));
																		                                    nouveauTour.put("statut", "attente");
																		                                    nouveauTour.put("votes", new HashMap<String, Integer>());
																		                                    nouveauTour.put("votants", new ArrayList<String>());
																		                                    unite.put("tourSuivant", nouveauTour);
																		                                    redessiner();
																		                                    broadcastEtat();
																		                                }
																                            })
														                            .setNegativeButton("Annuler", null)
														                            .show();
														                    }
												                });
										                parentBox.addView(btnSecondTour);
										            }
								
								            if (estRacine && listeParente != null && sousSectionOuverte[0] == null) {
										                Button btnSupprimer = new Button(AdminActivity.this);
										                btnSupprimer.setText("🗑 Supprimer");
										                btnSupprimer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
										                btnSupprimer.setOnClickListener(new View.OnClickListener() {
												                    public void onClick(View v) {
														                        new AlertDialog.Builder(AdminActivity.this)
														                            .setTitle("Confirmer la suppression")
														                            .setMessage("Supprimer \"" + (String) unite.get("titre") + "\" et tous ses résultats ? Cette action est irréversible.")
														                            .setPositiveButton("Supprimer", new DialogInterface.OnClickListener() {
																                                public void onClick(DialogInterface dialog, int which) {
																		                                    listeParente.remove(unite);
																		                                    redessiner();
																		                                    broadcastEtat();
																		                                    Toast.makeText(AdminActivity.this, "Supprimé", Toast.LENGTH_SHORT).show();
																		                                }
																                            })
														                            .setNegativeButton("Annuler", null)
														                            .show();
														                    }
												                });
										                parentBox.addView(btnSupprimer);
										
										                Button btnRefaire = new Button(AdminActivity.this);
										                btnRefaire.setText("↻ Modifier / Refaire le vote");
										                btnRefaire.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
										                btnRefaire.setOnClickListener(new View.OnClickListener() {
												                    public void onClick(View v) {
														                        new AlertDialog.Builder(AdminActivity.this)
														                            .setTitle("Refaire le vote")
														                            .setMessage("Les résultats actuels de \"" + (String) unite.get("titre") + "\" seront effacés (candidats et réglages conservés). Continuer ?")
														                            .setPositiveButton("Confirmer", new DialogInterface.OnClickListener() {
																                                public void onClick(DialogInterface dialog, int which) {
																		                                    unite.put("statut", "attente");
																		                                    unite.put("votes", new HashMap<String, Integer>());
																		                                    unite.put("votants", new ArrayList<String>());
																		                                    unite.remove("tourSuivant");
																		                                    unite.remove("designes");
																		                                    redessiner();
																		                                    broadcastEtat();
																		                                    Toast.makeText(AdminActivity.this, "Prêt à être modifié / rouvert", Toast.LENGTH_SHORT).show();
																		                                }
																                            })
														                            .setNegativeButton("Annuler", null)
														                            .show();
														                    }
												                });
										                parentBox.addView(btnRefaire);
										            }
								        }
						
						        if (unite.containsKey("tourSuivant")) {
								            LinearLayout sousBoxTour = new LinearLayout(AdminActivity.this);
								            sousBoxTour.setOrientation(LinearLayout.VERTICAL);
								            sousBoxTour.setPadding(30, 15, 10, 15);
								            afficherBlocVotable(sousBoxTour, (HashMap<String, Object>) unite.get("tourSuivant"), false, null, false, posteBase);
								            parentBox.addView(sousBoxTour);
								        }
						    }
				
				    void redessiner() {
				        if (enConsultation) {
				            afficherEcranConsultation();
				            if (!nomProjet[0].isEmpty()) sauvegarderProjetFichier(false);
				            return;
				        }
				        if (containerActif == null) return;
				        containerActif.removeAllViews();
				        int nbCartes = 0;
				        for (int s = 0; s < sections.size(); s++) {
				            final HashMap<String, Object> section = sections.get(s);
				            final String titreSection = (String) section.get("titre");
				            final ArrayList<HashMap<String, Object>> sousSections =
				                (ArrayList<HashMap<String, Object>>) section.get("sousSections");
				            boolean aDesSous = sousSections.size() > 0;
				            if (!aDesSous && estResolu(section)) continue;
				            int restants = 0;
				            for (int k = 0; k < sousSections.size(); k++) if (!estResolu(sousSections.get(k))) restants++;
				            if (aDesSous && restants == 0) continue; // section entierement cloturee -> Consultation
				            nbCartes++;
				            String rom = romain(s + 1);

				            LinearLayout sectionBox = new LinearLayout(AdminActivity.this);
				            sectionBox.setOrientation(LinearLayout.VERTICAL);
				            sectionBox.setPadding(20, 12, 20, 20);
				            sectionBox.setBackgroundColor(Color.parseColor("#F0F0F0"));
				            LinearLayout.LayoutParams lpBox = new LinearLayout.LayoutParams(
				                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				            lpBox.setMargins(0, 0, 0, 30);
				            sectionBox.setLayoutParams(lpBox);

				            final boolean plie = sectionPlie(section);
				            String resume;
				            if (aDesSous) {
				                resume = "   (" + restants + " à traiter)";
				            } else {
				                resume = "   " + badgeDe(section);
				            }
				            LinearLayout ligneTitre = new LinearLayout(AdminActivity.this);
				            ligneTitre.setOrientation(LinearLayout.HORIZONTAL);
				            ligneTitre.setGravity(Gravity.CENTER_VERTICAL);
				            TextView entete = new TextView(AdminActivity.this);
				            entete.setText((plie ? "▶  " : "▼  ") + rom + ".  " + titreSection + resume);
				            entete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
				            entete.setTypeface(null, Typeface.BOLD);
				            entete.setTextColor(Color.parseColor("#283593"));
				            entete.setPadding(0, 14, 0, 14);
				            entete.setOnClickListener(new View.OnClickListener() {
				                public void onClick(View v) {
				                    section.put("plie", Boolean.valueOf(!plie));
				                    redessiner();
				                }
				            });
				            ligneTitre.addView(entete, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
				            Button btnSupprSection = new Button(AdminActivity.this);
				            btnSupprSection.setText("🗑");
				            btnSupprSection.setTag("mini");
				            btnSupprSection.setOnClickListener(new View.OnClickListener() {
				                public void onClick(View v) {
				                    confirmerSuppressionSection(section);
				                }
				            });
				            ligneTitre.addView(btnSupprSection, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				            sectionBox.addView(ligneTitre);

				            if (!plie) {
				                if (!aDesSous) {
				                    section.put("sansTitre", Boolean.TRUE);
				                    afficherBlocVotable(sectionBox, section, true, sections, true, titreSection);
				                    section.remove("sansTitre");
				                } else {
				                    for (int ss = 0; ss < sousSections.size(); ss++) {
				                        final HashMap<String, Object> sousSection = sousSections.get(ss);
				                        if (estResolu(sousSection)) continue;
				                        sousSection.put("numLabel", (ss + 1) + ". ");
				                        LinearLayout sousBox = new LinearLayout(AdminActivity.this);
				                        sousBox.setOrientation(LinearLayout.VERTICAL);
				                        sousBox.setPadding(30, 15, 10, 15);
				                        afficherBlocVotable(sousBox, sousSection, false, sousSections, true, (String) sousSection.get("titre") + " " + titreSection);
				                        sectionBox.addView(sousBox);
				                    }
				                }
				                String statutSection = (String) section.get("statut");
				                boolean sectionEncoreVierge = ((ArrayList<String>) section.get("candidats")).size() == 0
				                    && statutSection.equals("attente");
				                if (sousSectionOuverte[0] == null && sectionEncoreVierge) {
				                    ajouterBoutonSousSection(sectionBox, section, sousSections);
				                }
				            }
				            containerActif.addView(sectionBox);
				        }
				        if (nbCartes == 0) {
				            TextView aucun = new TextView(AdminActivity.this);
				            aucun.setText("Aucun scrutin en cours.\nAjoutez une section avec ➕, ou ouvrez 🗂 Consultation pour revoir les scrutins terminés.");
				            aucun.setTextColor(Color.parseColor("#888888"));
				            aucun.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				            aucun.setPadding(0, 30, 0, 30);
				            containerActif.addView(aucun);
				        }
				        etatCache = construireEtatJson();
if (btnVoterSticky != null) {
				            if (sousSectionOuverte[0] == null) {
				                btnVoterSticky.setEnabled(false);
				                btnVoterSticky.setText("Voter (aucun vote ouvert)");
				            } else {
				                ArrayList<String> votantsActuels = (ArrayList<String>) sousSectionOuverte[0].get("votants");
				                if (votantsActuels.contains(nomAdmin)) {
				                    btnVoterSticky.setEnabled(false);
				                    btnVoterSticky.setText("Déjà voté");
				                } else {
				                    btnVoterSticky.setEnabled(true);
				                    btnVoterSticky.setText("🗳 Voter maintenant");
				                }
				            }
				        }
				        if (scrollVersBasDemande && scrollPrincipal != null) {
				            scrollVersBasDemande = false;
				            scrollPrincipal.post(new Runnable() {
				                public void run() {
				                    scrollPrincipal.fullScroll(View.FOCUS_DOWN);
				                }
				            });
				        }
				        if (!nomProjet[0].isEmpty()) {
				            sauvegarderProjetFichier(false);
				        }
				    }

				    boolean appartient(HashMap<String, Object> u, HashMap<String, Object> cible) {
				        if (u == cible) return true;
				        if (u.containsKey("tourSuivant") && appartient((HashMap<String, Object>) u.get("tourSuivant"), cible)) return true;
				        ArrayList<HashMap<String, Object>> ss = (ArrayList<HashMap<String, Object>>) u.get("sousSections");
				        if (ss != null) {
				            for (int i = 0; i < ss.size(); i++) if (appartient(ss.get(i), cible)) return true;
				        }
				        return false;
				    }

				    void confirmerSuppressionSection(final HashMap<String, Object> section) {
				        HashMap<String, Object> ouverte = sousSectionOuverte[0];
				        if (ouverte != null && appartient(section, ouverte)) {
				            Toast.makeText(AdminActivity.this, "Fermez d'abord le vote en cours dans cette section", Toast.LENGTH_LONG).show();
				            return;
				        }
				        new AlertDialog.Builder(AdminActivity.this)
				            .setTitle("Supprimer la section ?")
				            .setMessage("« " + section.get("titre") + " » et tous ses scrutins seront supprimés définitivement.")
				            .setPositiveButton("Supprimer", new DialogInterface.OnClickListener() {
				                public void onClick(DialogInterface dialog, int which) {
				                    for (int i = 0; i < sections.size(); i++) {
				                        if (sections.get(i) == section) {
				                            sections.remove(i);
				                            break;
				                        }
				                    }
				                    redessiner();
				                    broadcastEtat();
				                    Toast.makeText(AdminActivity.this, "Section supprimée", Toast.LENGTH_SHORT).show();
				                }
				            })
				            .setNegativeButton("Annuler", null)
				            .show();
				    }

				    void ajouterBoutonSousSection(LinearLayout box, final HashMap<String, Object> section, final ArrayList<HashMap<String, Object>> sousSections) {
				        Button btnAjouterSousSection = new Button(AdminActivity.this);
				        btnAjouterSousSection.setText("+ Sous-section");
				        btnAjouterSousSection.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
				        btnAjouterSousSection.setOnClickListener(new View.OnClickListener() {
				            public void onClick(View v) {
				                final EditText inputTitre = new EditText(AdminActivity.this);
				                inputTitre.setHint("Titre (ex: Président)");
				                new AlertDialog.Builder(AdminActivity.this)
				                    .setTitle("Nouvelle sous-section")
				                    .setView(inputTitre)
				                    .setPositiveButton("Ajouter", new DialogInterface.OnClickListener() {
				                        public void onClick(DialogInterface dialog, int which) {
				                            String titreS = inputTitre.getText().toString().trim();
				                            if (!titreS.isEmpty()) {
				                                HashMap<String, Object> nouvelle = new HashMap<String, Object>();
				                                nouvelle.put("titre", titreS);
				                                nouvelle.put("nbMax", 1);
				                                nouvelle.put("nbAdmis", 1);
				                                nouvelle.put("candidats", new ArrayList<String>());
				                                nouvelle.put("statut", "attente");
				                                nouvelle.put("votes", new HashMap<String, Integer>());
				                                nouvelle.put("votants", new ArrayList<String>());
				                                nouvelle.put("modeRoles", false);
				                                nouvelle.put("roles", new ArrayList<HashMap<String, Object>>());
				                                sousSections.add(nouvelle);
				                                section.put("plie", Boolean.FALSE);
				                                scrollVersBasDemande = true;
				                                redessiner();
				                            }
				                        }
				                    })
				                    .setNegativeButton("Annuler", null)
				                    .show();
				            }
				        });
				        box.addView(btnAjouterSousSection);
				    }

    void ajouterEntreesResultat(ArrayList<Object[]> entries, HashMap<String, Object> unite, String posteBase) {
						        String statut = (String) unite.get("statut");
						        if (statut.equals("designe")) {
								            entries.add(new Object[]{(String) unite.get("titre") + " — désigné(s)", "TITRE_UNITE"});
								            ArrayList<String> designes = (ArrayList<String>) unite.get("designes");
								            for (int i = 0; i < designes.size(); i++) entries.add(new Object[]{"✓ " + designes.get(i), "ELU"});
								            entries.add(new Object[]{"", "BLANK"});
								            return;
								        }
						        if (!statut.equals("ferme")) return;
						        if (Boolean.TRUE.equals(unite.get("modeRoles"))) {
								            int nbAdmis = (Integer) unite.get("nbAdmis");
								            entries.add(new Object[]{(String) unite.get("titre") + " — " + nbAdmis + " postes", "TITRE_UNITE"});
								            ArrayList<Object[]> affectation = resoudreRoles(unite);
								            if (affectation == null) {
										                entries.add(new Object[]{"Départage de poste en cours", "INFO"});
										            } else {
										                ArrayList<HashMap<String, Object>> roles = (ArrayList<HashMap<String, Object>>) unite.get("roles");
										                for (int r = 0; r < roles.size(); r++) {
												                    String nomRole = (String) roles.get(r).get("nom");
												                    for (int i = 0; i < affectation.size(); i++) {
														                        Object[] a = affectation.get(i);
														                        if (((String) a[2]).equals(nomRole)) entries.add(new Object[]{"✓ " + (String) a[0] + " — " + nomRole, "ELU"});
														                    }
												                }
										            }
								            entries.add(new Object[]{"", "BLANK"});
								            return;
								        }
						        int nbAdmis = unite.containsKey("nbAdmis") ? (Integer) unite.get("nbAdmis") : (Integer) unite.get("nbMax");
						        entries.add(new Object[]{(String) unite.get("titre") + " — " + nbAdmis + " admis", "TITRE_UNITE"});
						        ArrayList<Object[]> elus = calculerElus(unite);
						        if (elus == null) {
								            entries.add(new Object[]{"Départage en cours (égalité)", "INFO"});
								        } else {
								            for (int i = 0; i < elus.size(); i++) entries.add(new Object[]{"✓ " + (String) elus.get(i)[0], "ELU"});
								        }
						        entries.add(new Object[]{"", "BLANK"});
						    }
				
				    void afficherEcranResultats() {
						        enConsultation = false;
						        screenContainer.removeAllViews();
						        ScrollView scroll = new ScrollView(AdminActivity.this);
						        LinearLayout rootLayout = new LinearLayout(AdminActivity.this);
						        rootLayout.setOrientation(LinearLayout.VERTICAL);
						        rootLayout.setPadding(40, 80, 40, 80);
						        rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView titre = new TextView(AdminActivity.this);
						        titre.setText("Résultats (élus) : " + nomProjet[0]);
						        titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
						        titre.setPadding(0, 0, 0, 30);
						        rootLayout.addView(titre);
						
						        final ArrayList<Object[]> entries = new ArrayList<Object[]>();
						        for (int s = 0; s < sections.size(); s++) {
								            HashMap<String, Object> section = sections.get(s);
								            ArrayList<HashMap<String, Object>> sousSections = (ArrayList<HashMap<String, Object>>) section.get("sousSections");
								            if (sousSections.size() == 0) {
										                ajouterEntreesResultat(entries, section, (String) section.get("titre"));
										            } else {
										                entries.add(new Object[]{(String) section.get("titre"), "TITRE_SECTION"});
										                for (int ss = 0; ss < sousSections.size(); ss++) {
												                    ajouterEntreesResultat(entries, sousSections.get(ss), (String) sousSections.get(ss).get("titre") + " " + (String) section.get("titre"));
												                }
										            }
								        }
						
						        if (entries.size() == 0) {
								            TextView vide = new TextView(AdminActivity.this);
								            vide.setText("Aucun vote clôturé pour le moment.");
								            vide.setTextColor(Color.parseColor("#888888"));
								            rootLayout.addView(vide);
								        } else {
								            for (int i = 0; i < entries.size(); i++) {
										                Object[] e = entries.get(i);
										                String texte = (String) e[0];
										                String type = (String) e[1];
										                if (type.equals("BLANK")) {
												                    TextView spacer = new TextView(AdminActivity.this);
												                    spacer.setText(" ");
												                    spacer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
												                    rootLayout.addView(spacer);
												                    continue;
												                }
										                TextView t = new TextView(AdminActivity.this);
										                t.setText(texte);
										                if (type.equals("TITRE_SECTION")) {
												                    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
												                    t.setTypeface(null, Typeface.BOLD);
												                    t.setTextColor(Color.parseColor("#1A237E"));
												                    t.setPadding(0, 15, 0, 4);
												                } else if (type.equals("TITRE_UNITE")) {
												                    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
												                    t.setTypeface(null, Typeface.BOLD);
												                    t.setTextColor(Color.parseColor("#303F9F"));
												                    t.setPadding(20, 10, 0, 2);
												                } else if (type.equals("ELU")) {
												                    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
												                    t.setTextColor(Color.parseColor("#212121"));
												                    t.setPadding(40, 2, 0, 2);
												                } else if (type.equals("INFO")) {
												                    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
												                    t.setTextColor(Color.parseColor("#CC6600"));
												                    t.setPadding(40, 2, 0, 2);
												                }
										                rootLayout.addView(t);
										            }
								        }
						
						        Button btnExport = new Button(AdminActivity.this);
						        btnExport.setText("Exporter en PDF");
						        LinearLayout.LayoutParams lpExp = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpExp.setMargins(0, 40, 0, 20);
						        btnExport.setLayoutParams(lpExp);
						        rootLayout.addView(btnExport);
						        btnExport.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                exporterPdf();
										            }
								        });
						
						        Button btnRetour = new Button(AdminActivity.this);
						        btnRetour.setText("Retour");
						        rootLayout.addView(btnRetour);
						        btnRetour.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                afficherEcranPrincipal();
										            }
								        });
						
						        scroll.addView(rootLayout);
						        screenContainer.addView(scroll);
						    }
				
				    
    JSONObject serialiserUnite(HashMap<String, Object> unite) {
						        JSONObject o = new JSONObject();
						        try {
								            o.put("titre", (String) unite.get("titre"));
								            o.put("statut", (String) unite.get("statut"));
								            o.put("nbMax", unite.get("nbMax"));
								            o.put("nbAdmis", unite.containsKey("nbAdmis") ? unite.get("nbAdmis") : unite.get("nbMax"));
								            ArrayList<String> cands = (ArrayList<String>) unite.get("candidats");
								            JSONArray candArr = new JSONArray();
								            for (int i = 0; i < cands.size(); i++) candArr.put(cands.get(i));
								            o.put("candidats", candArr);
								            HashMap<String, Integer> votesMap = (HashMap<String, Integer>) unite.get("votes");
								            JSONObject votesObj = new JSONObject();
								            for (int i = 0; i < cands.size(); i++) {
										                String c = cands.get(i);
										                int v = votesMap.containsKey(c) ? votesMap.get(c) : 0;
										                votesObj.put(c, v);
										            }
								            o.put("votes", votesObj);
								            ArrayList<String> votantsList = (ArrayList<String>) unite.get("votants");
								            JSONArray votantsArr = new JSONArray();
								            if (votantsList != null) {
										                for (int i = 0; i < votantsList.size(); i++) votantsArr.put(votantsList.get(i));
										            }
								            o.put("votants", votantsArr);
								            o.put("modeRoles", unite.containsKey("modeRoles") ? unite.get("modeRoles") : false);
								            if (unite.containsKey("roles")) {
										                ArrayList<HashMap<String, Object>> rolesList = (ArrayList<HashMap<String, Object>>) unite.get("roles");
										                JSONArray rolesArr = new JSONArray();
										                for (int i = 0; i < rolesList.size(); i++) {
												                    JSONObject ro = new JSONObject();
												                    ro.put("nom", (String) rolesList.get(i).get("nom"));
												                    ro.put("nombre", (Integer) rolesList.get(i).get("nombre"));
												                    rolesArr.put(ro);
												                }
										                o.put("roles", rolesArr);
										            }
								            if (unite.containsKey("designes")) {
										                ArrayList<String> des = (ArrayList<String>) unite.get("designes");
										                JSONArray desArr = new JSONArray();
										                for (int i = 0; i < des.size(); i++) desArr.put(des.get(i));
										                o.put("designes", desArr);
										            }
								            if (unite.containsKey("elimines")) {
    ArrayList<HashMap<String, Object>> elL = (ArrayList<HashMap<String, Object>>) unite.get("elimines");
    JSONArray elArr = new JSONArray();
    for (int i = 0; i < elL.size(); i++) {
        JSONObject eo = new JSONObject();
        eo.put("nom", (String) elL.get(i).get("nom"));
        eo.put("cause", (String) elL.get(i).get("cause"));
        String rm = (String) elL.get(i).get("remplacant");
        eo.put("remplacant", rm == null ? "" : rm);
        elArr.put(eo);
    }
    o.put("elimines", elArr);
}
if (unite.containsKey("tourSuivant")) {
										                o.put("tourSuivant", serialiserUnite((HashMap<String, Object>) unite.get("tourSuivant")));
										            }
								        } catch (Exception e) {}
						        return o;
						    }
				
				    JSONArray serialiserSections() {
						        JSONArray arr = new JSONArray();
						        for (int i = 0; i < sections.size(); i++) {
								            HashMap<String, Object> section = sections.get(i);
								            ArrayList<HashMap<String, Object>> sousSections =
								                (ArrayList<HashMap<String, Object>>) section.get("sousSections");
								            JSONObject secObj = new JSONObject();
								            try {
										                secObj.put("titre", (String) section.get("titre"));
										                if (sousSections != null && sousSections.size() > 0) {
												                    JSONArray ssArr = new JSONArray();
												                    for (int j = 0; j < sousSections.size(); j++) {
														                        ssArr.put(serialiserUnite(sousSections.get(j)));
														                    }
												                    secObj.put("sousSections", ssArr);
												                } else {
												                    secObj.put("unite", serialiserUnite(section));
												                }
										            } catch (Exception e) {}
								            arr.put(secObj);
								        }
						        return arr;
						    }
				
				    JSONArray serialiserSessions() {
						        JSONArray arr = new JSONArray();
						        for (int i = 0; i < sessions.size(); i++) {
								            HashMap<String, Object> s = sessions.get(i);
								            JSONObject o = new JSONObject();
								            try {
										                o.put("label", (String) s.get("label"));
										                ArrayList<String> presents = (ArrayList<String>) s.get("presents");
										                JSONArray pArr = new JSONArray();
										                for (int j = 0; j < presents.size(); j++) pArr.put(presents.get(j));
										                o.put("presents", pArr);
										            } catch (Exception e) {}
								            arr.put(o);
								        }
						        return arr;
						    }
				
				    void chargerSessionsDepuisJson(JSONArray arr) throws Exception {
						        sessions.clear();
						        for (int i = 0; i < arr.length(); i++) {
								            JSONObject o = arr.getJSONObject(i);
								            HashMap<String, Object> s = new HashMap<String, Object>();
								            s.put("label", o.getString("label"));
								            ArrayList<String> presents = new ArrayList<String>();
								            JSONArray pArr = o.getJSONArray("presents");
								            for (int j = 0; j < pArr.length(); j++) presents.add(pArr.getString(j));
								            s.put("presents", presents);
								            sessions.add(s);
								        }
						    }
				
				    HashMap<String, Object> uniteFromJson(JSONObject o) throws Exception {
						        HashMap<String, Object> u = new HashMap<String, Object>();
						        u.put("titre", o.getString("titre"));
						        u.put("statut", o.getString("statut"));
						        u.put("nbMax", o.getInt("nbMax"));
						        u.put("nbAdmis", o.has("nbAdmis") ? o.getInt("nbAdmis") : o.getInt("nbMax"));
						        ArrayList<String> cands = new ArrayList<String>();
						        JSONArray candArr = o.getJSONArray("candidats");
						        for (int i = 0; i < candArr.length(); i++) cands.add(candArr.getString(i));
						        u.put("candidats", cands);
						        HashMap<String, Integer> votesMap = new HashMap<String, Integer>();
						        JSONObject votesObj = o.getJSONObject("votes");
						        for (int i = 0; i < cands.size(); i++) {
								            String c = cands.get(i);
								            if (votesObj.has(c)) votesMap.put(c, votesObj.getInt(c));
								        }
						        u.put("votes", votesMap);
						        ArrayList<String> votants = new ArrayList<String>();
						        if (o.has("votants")) {
								            JSONArray votantsArr = o.getJSONArray("votants");
								            for (int i = 0; i < votantsArr.length(); i++) votants.add(votantsArr.getString(i));
								        }
						        u.put("votants", votants);
						        u.put("modeRoles", o.has("modeRoles") ? o.getBoolean("modeRoles") : false);
						        ArrayList<HashMap<String, Object>> roles = new ArrayList<HashMap<String, Object>>();
						        if (o.has("roles")) {
								            JSONArray rolesArr = o.getJSONArray("roles");
								            for (int i = 0; i < rolesArr.length(); i++) {
										                JSONObject ro = rolesArr.getJSONObject(i);
										                HashMap<String, Object> role = new HashMap<String, Object>();
										                role.put("nom", ro.getString("nom"));
										                role.put("nombre", ro.getInt("nombre"));
										                roles.add(role);
										            }
								        }
						        u.put("roles", roles);
						        if (o.has("designes")) {
								            ArrayList<String> designes = new ArrayList<String>();
								            JSONArray desArr = o.getJSONArray("designes");
								            for (int i = 0; i < desArr.length(); i++) designes.add(desArr.getString(i));
								            u.put("designes", designes);
								        }
						        if (o.has("elimines")) {
    ArrayList<HashMap<String, Object>> elL = new ArrayList<HashMap<String, Object>>();
    JSONArray elArr = o.getJSONArray("elimines");
    for (int i = 0; i < elArr.length(); i++) {
        JSONObject eo = elArr.getJSONObject(i);
        HashMap<String, Object> e = new HashMap<String, Object>();
        e.put("nom", eo.getString("nom"));
        e.put("cause", eo.getString("cause"));
        e.put("remplacant", eo.optString("remplacant", ""));
        elL.add(e);
    }
    u.put("elimines", elL);
}
if (o.has("tourSuivant")) {
								            u.put("tourSuivant", uniteFromJson(o.getJSONObject("tourSuivant")));
								        }
						        return u;
						    }
				
				    void relierRacineRoles(HashMap<String, Object> racine, HashMap<String, Object> noeud) {
						        if (noeud.containsKey("tourSuivant")) {
								            HashMap<String, Object> suivant = (HashMap<String, Object>) noeud.get("tourSuivant");
								            suivant.put("racineRoles", racine);
								            relierRacineRoles(racine, suivant);
								        }
						    }
				
				    void chargerSectionsDepuisJson(JSONArray arr) throws Exception {
						        sections.clear();
						        for (int i = 0; i < arr.length(); i++) {
								            JSONObject secObj = arr.getJSONObject(i);
								            HashMap<String, Object> section;
								            if (secObj.has("sousSections")) {
										                section = new HashMap<String, Object>();
										                section.put("titre", secObj.getString("titre"));
										                section.put("candidats", new ArrayList<String>());
										                section.put("votes", new HashMap<String, Integer>());
										                section.put("votants", new ArrayList<String>());
										                section.put("nbMax", 1);
										                section.put("nbAdmis", 1);
										                section.put("statut", "attente");
										                section.put("modeRoles", false);
										                section.put("roles", new ArrayList<HashMap<String, Object>>());
										                ArrayList<HashMap<String, Object>> sousSections = new ArrayList<HashMap<String, Object>>();
										                JSONArray ssArr = secObj.getJSONArray("sousSections");
										                for (int j = 0; j < ssArr.length(); j++) {
												                    sousSections.add(uniteFromJson(ssArr.getJSONObject(j)));
												                }
										                section.put("sousSections", sousSections);
										            } else {
										                section = uniteFromJson(secObj.getJSONObject("unite"));
										                section.put("sousSections", new ArrayList<HashMap<String, Object>>());
										            }
								            sections.add(section);
								        }
						        for (int i = 0; i < sections.size(); i++) {
								            HashMap<String, Object> section = sections.get(i);
								            if (Boolean.TRUE.equals(section.get("modeRoles"))) {
										                relierRacineRoles(section, section);
										            }
								            ArrayList<HashMap<String, Object>> ss = (ArrayList<HashMap<String, Object>>) section.get("sousSections");
								            for (int j = 0; j < ss.size(); j++) {
										                HashMap<String, Object> sub = ss.get(j);
										                if (Boolean.TRUE.equals(sub.get("modeRoles"))) {
												                    relierRacineRoles(sub, sub);
												                }
										            }
								        }
						    }
				
				    JSONObject serialiserProjetComplet() {
						        JSONObject o = new JSONObject();
						        try {
								            o.put("projet", nomProjet[0]);
								            o.put("sections", serialiserSections());
								            o.put("sessions", serialiserSessions());
								            SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
								            o.put("membres", new JSONArray(prefs.getString("membres_courant", "[]")));
								        } catch (Exception e) {}
						        return o;
						    }
				
				    void sauvegarderProjetFichier(final boolean avecToast) {
						        try {
								            File dossier = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
								            if (!dossier.exists()) dossier.mkdirs();
								            String nomFichier = "projet_" + nomProjet[0].replace(" ", "_") + ".json";
								            File fichier = new File(dossier, nomFichier);
								            FileOutputStream fos = new FileOutputStream(fichier);
								            fos.write(serialiserProjetComplet().toString().getBytes());
								            fos.close();
								            if (avecToast) {
										                Toast.makeText(AdminActivity.this, "Projet sauvegardé : " + nomFichier, Toast.LENGTH_SHORT).show();
										            }
								        } catch (Exception e) {
								            if (avecToast) {
										                Toast.makeText(AdminActivity.this, "Erreur sauvegarde : " + e.getMessage(), Toast.LENGTH_LONG).show();
										            }
								        }
						    }
				
				    void listerEtChargerProjets() {
						        try {
								            File dossier = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
								            if (!dossier.exists()) {
										                Toast.makeText(AdminActivity.this, "Aucun projet sauvegardé trouvé", Toast.LENGTH_SHORT).show();
										                return;
										            }
								            File[] fichiers = dossier.listFiles();
								            final ArrayList<File> jsonFichiers = new ArrayList<File>();
								            if (fichiers != null) {
										                for (int i = 0; i < fichiers.length; i++) {
												                    if (fichiers[i].getName().startsWith("projet_") && fichiers[i].getName().endsWith(".json")) {
														                        jsonFichiers.add(fichiers[i]);
														                    }
												                }
										            }
								            if (jsonFichiers.size() == 0) {
										                Toast.makeText(AdminActivity.this, "Aucun projet sauvegardé trouvé", Toast.LENGTH_SHORT).show();
										                return;
										            }
								            final String[] noms = new String[jsonFichiers.size()];
								            for (int i = 0; i < jsonFichiers.size(); i++) {
										                noms[i] = jsonFichiers.get(i).getName().replace("projet_", "").replace(".json", "").replace("_", " ");
										            }
								            new AlertDialog.Builder(AdminActivity.this)
								                .setTitle("Charger un projet")
								                .setItems(noms, new DialogInterface.OnClickListener() {
										                    public void onClick(DialogInterface dialog, int which) {
												                        try {
														                            File f = jsonFichiers.get(which);
														                            BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(f)));
														                            StringBuilder sb = new StringBuilder();
														                            String ligne;
														                            while ((ligne = reader.readLine()) != null) sb.append(ligne);
														                            reader.close();
														                            JSONObject data = new JSONObject(sb.toString());
														                            nomProjet[0] = data.getString("projet");
														                            chargerSectionsDepuisJson(data.getJSONArray("sections"));
														                            if (data.has("sessions")) chargerSessionsDepuisJson(data.getJSONArray("sessions"));
														                            SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
														                            if (data.has("membres")) {
																                                prefs.edit().putString("membres_courant", data.getJSONArray("membres").toString()).apply();
																                            } else {
																                                prefs.edit().putString("membres_courant", "[]").apply();
																                            }
														                            Toast.makeText(AdminActivity.this, "Projet chargé : " + nomProjet[0], Toast.LENGTH_SHORT).show();
broadcastEtat();
														                            afficherEcranPrincipal();
														                        } catch (Exception e) {
														                            Toast.makeText(AdminActivity.this, "Erreur chargement : " + e.getMessage(), Toast.LENGTH_LONG).show();
														                        }
												                    }
										                })
								                .setNegativeButton("Annuler", null)
								                .show();
								        } catch (Exception e) {
								            Toast.makeText(AdminActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
								        }
						    }
				
				    volatile String etatCache = null;

String construireEtatJson() {
    JSONObject etat = new JSONObject();
    try {
        etat.put("type", "etat");
        etat.put("projet", nomProjet[0]);
        etat.put("sections", serialiserSections());
        SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
        etat.put("listeMembres", new JSONArray(prefs.getString("membres_courant", "[]")));
    } catch (Exception e) {}
    return etat.toString();
}

void broadcastEtat() {
    final String msg = construireEtatJson();
    etatCache = msg;
    new Thread(new Runnable() {
        public void run() {
            ArrayList<PrintWriter> morts = new ArrayList<PrintWriter>();
            for (PrintWriter w : clients) {
                try {
                    w.println(msg);
                    if (w.checkError()) morts.add(w);
                } catch (Exception e) { morts.add(w); }
            }
            clients.removeAll(morts);
        }
    }).start();
}

void envoyerEtatA(PrintWriter pw) {
    String m = etatCache;
    if (m == null) m = construireEtatJson();
    try { pw.println(m); } catch (Exception e) {}
}

void traiterVote(String nomParticipant, ArrayList<String> choix) {
						        HashMap<String, Object> unite = sousSectionOuverte[0];
						        if (unite == null) return;
						        ArrayList<String> votants = (ArrayList<String>) unite.get("votants");
						        if (votants.contains(nomParticipant)) return;
						        votants.add(nomParticipant);
						        HashMap<String, Integer> votes = (HashMap<String, Integer>) unite.get("votes");
						        for (int i = 0; i < choix.size(); i++) {
								            String c = choix.get(i);
								            int actuel = votes.containsKey(c) ? votes.get(c) : 0;
								            votes.put(c, actuel + 1);
								        }
						        runOnUiThread(new Runnable() {
								            public void run() {
										                redessiner();
										            }
								        });
						        broadcastEtat();
						    }
				
				    void demarrerServeur() {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    ServerSocket serverSocket = null;
                    try {
                        serverSocket = new ServerSocket();
                        serverSocket.setReuseAddress(true);
                        serverSocket.bind(new java.net.InetSocketAddress(8988));
                        while (true) {
                            try {
                                final Socket clientSocket = serverSocket.accept();
                                clientSocket.setKeepAlive(true);
                                clientSocket.setTcpNoDelay(true);
                                final PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                                clients.add(writer);
                                envoyerEtatA(writer);
                                new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                                            String ligne;
                                            while ((ligne = reader.readLine()) != null) {
                                                try {
                                                    JSONObject obj = new JSONObject(ligne);
                                                    if (obj.getString("type").equals("vote")) {
                                                        String nomP = obj.getString("nom");
                                                        JSONArray choixArr = obj.getJSONArray("choix");
                                                        ArrayList<String> choix = new ArrayList<String>();
                                                        for (int i = 0; i < choixArr.length(); i++) choix.add(choixArr.getString(i));
                                                        String titreVote = obj.optString("titre", "");
                                                        HashMap<String, Object> ouverteMaintenant = sousSectionOuverte[0];
                                                        if (titreVote.length() == 0 || (ouverteMaintenant != null && titreVote.equals((String) ouverteMaintenant.get("titre")))) {
                                                            traiterVote(nomP, choix);
                                                        }
                                                        try {
                                                            JSONObject ack = new JSONObject();
                                                            ack.put("type", "ack");
                                                            ack.put("nom", nomP);
                                                            ack.put("titre", titreVote);
                                                            writer.println(ack.toString());
                                                        } catch (Exception exAck) {}
                                                    }
                                                } catch (Exception e) {}
                                            }
                                        } catch (Exception e) {}
                                        clients.remove(writer);
                                        try { clientSocket.close(); } catch (Exception e2) {}
                                    }
                                }).start();
                            } catch (Exception exClient) {
                                // un client qui se coupe ne doit JAMAIS arreter le serveur
                                if (serverSocket.isClosed()) break;
                            }
                        }
                    } catch (Exception e) {
                    } finally {
                        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e3) {}
                    }
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { return; }
                }
            }
        }).start();
        // battement de coeur : detecte les connexions mortes des deux cotes
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try { Thread.sleep(8000); } catch (InterruptedException ie) { return; }
                    ArrayList<PrintWriter> morts = new ArrayList<PrintWriter>();
                    for (PrintWriter w : clients) {
                        try {
                            w.println("{\"type\":\"ping\"}");
                            if (w.checkError()) morts.add(w);
                        } catch (Exception e) { morts.add(w); }
                    }
                    clients.removeAll(morts);
                }
            }
        }).start();
    }
    }

        final Ecrans ecrans = new Ecrans();
		diffuserEtat = new Runnable() {
			public void run() {
				ecrans.broadcastEtat();
			}
		};
		ecrans.afficherCreationProjet();
		ecrans.demarrerServeur();
	}
	
	private void initializeLogic() {
	}
	
	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		if (_requestCode == 501 && _resultCode == RESULT_OK && _data != null) {
				    try {
						        Uri uri = _data.getData();
						        InputStream is = getContentResolver().openInputStream(uri);
						        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						        ArrayList<String> nouveaux = new ArrayList<String>();
						        String ligne;
						        while ((ligne = reader.readLine()) != null) {
								            String n = ligne.trim();
								            if (!n.isEmpty()) nouveaux.add(n);
								        }
						        reader.close();
						
						        SharedPreferences prefs = getSharedPreferences("VoteMGR", MODE_PRIVATE);
						        String jsonActuel = prefs.getString("membres_courant", "[]");
						        ArrayList<String> membresActuels = new ArrayList<String>();
						        JSONArray arrActuel = new JSONArray(jsonActuel);
						        for (int i = 0; i < arrActuel.length(); i++) membresActuels.add(arrActuel.getString(i));
						
						        int ajoutes = 0;
						        for (int i = 0; i < nouveaux.size(); i++) {
								            if (!membresActuels.contains(nouveaux.get(i))) {
										                membresActuels.add(nouveaux.get(i));
										                ajoutes++;
										            }
								        }
						
						        JSONArray arrFinal = new JSONArray();
						        for (int i = 0; i < membresActuels.size(); i++) arrFinal.put(membresActuels.get(i));
						        prefs.edit().putString("membres_courant", arrFinal.toString()).apply();
						
						        Toast.makeText(this, ajoutes + " nouveaux noms ajoutés (" + membresActuels.size() + " au total)", Toast.LENGTH_LONG).show();
				if (diffuserEtat != null) diffuserEtat.run();
						    } catch (Exception e) {
						        Toast.makeText(this, "Erreur import : " + e.getMessage(), Toast.LENGTH_LONG).show();
						    }
		}
		if (_requestCode == 502) {
    if (_resultCode == RESULT_OK && _data != null && pdfEnAttente != null) {
        try {
            OutputStream os = getContentResolver().openOutputStream(_data.getData());
            os.write(pdfEnAttente);
            os.close();
            Toast.makeText(this, "PDF enregistré", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur enregistrement PDF : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    } else if (pdfEnAttente != null) {
        Toast.makeText(this, "PDF gardé dans le dossier de l'application", Toast.LENGTH_LONG).show();
    }
    pdfEnAttente = null;
}
switch (_requestCode) {
			
			default:
			break;
		}
	}
	
}