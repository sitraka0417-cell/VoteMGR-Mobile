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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Toast;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.HashMap;

public class VoteActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.vote);
		initialize(_savedInstanceState);
		initializeLogic();
		Skin3D.apply(this);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		final String nomParticipant = getIntent().getStringExtra("nom");
		final String ipServeur = getIntent().getStringExtra("ip");
		
		final LinearLayout screenContainer = new LinearLayout(this);
		screenContainer.setOrientation(LinearLayout.VERTICAL);
		setContentView(screenContainer);
		
		final LinearLayout barreHaut = new LinearLayout(this);
		barreHaut.setOrientation(LinearLayout.VERTICAL);
		barreHaut.setPadding(30, 60, 30, 15);
		barreHaut.setBackgroundColor(Color.parseColor("#E8EAF6"));
		
		final TextView titreProjet = new TextView(this);
		titreProjet.setText("Connexion...");
		titreProjet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		titreProjet.setTypeface(null, Typeface.BOLD);
		titreProjet.setPadding(0, 0, 0, 4);
		barreHaut.addView(titreProjet);
		
		final TextView statutConnexion = new TextView(this);
		statutConnexion.setText("Connexion au serveur " + ipServeur + "...");
		statutConnexion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		statutConnexion.setTextColor(Color.parseColor("#888888"));
		statutConnexion.setPadding(0, 0, 0, 10);
		barreHaut.addView(statutConnexion);
		
		final Button btnConsulterListe = new Button(this);
		btnConsulterListe.setText("📜 Consulter la liste");
		barreHaut.addView(btnConsulterListe);
		
		screenContainer.addView(barreHaut);
		
		final LinearLayout corpsContainer = new LinearLayout(this);
		corpsContainer.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams lpCorps = new LinearLayout.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
		corpsContainer.setLayoutParams(lpCorps);
		screenContainer.addView(corpsContainer);
		
		final ScrollView scroll = new ScrollView(this);
		final LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setPadding(40, 30, 40, 40);
		rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
		
		final LinearLayout zoneContenu = new LinearLayout(this);
		zoneContenu.setOrientation(LinearLayout.VERTICAL);
		rootLayout.addView(zoneContenu);
		
		scroll.addView(rootLayout);
		corpsContainer.addView(scroll);
		
		final LinearLayout panelVoteFixe = new LinearLayout(this);
		panelVoteFixe.setOrientation(LinearLayout.VERTICAL);
		panelVoteFixe.setPadding(30, 15, 30, 15);
		panelVoteFixe.setBackgroundColor(Color.parseColor("#FFF3E0"));
		panelVoteFixe.setVisibility(View.GONE);
		screenContainer.addView(panelVoteFixe);
		
		final PrintWriter[] writerRef = new PrintWriter[]{null};
		final String[] dernierTitreVote = new String[]{""};
		final JSONObject[] dernierEtat = new JSONObject[]{null};
final String[] voteEnAttente = new String[]{null};
final HashMap<String, ArrayList<String>> choixEnvoyes = new HashMap<String, ArrayList<String>>();
final HashMap<String, Boolean> confirmes = new HashMap<String, Boolean>();
final boolean[] listeOuverte = new boolean[]{false};
final HashMap<String, ArrayList<String>> selectionsEnCours = new HashMap<String, ArrayList<String>>();
		
		class Reseau {
				
				    String joinListe(ArrayList<String> l) {
						        String r = "";
						        for (int i = 0; i < l.size(); i++) {
								            if (i > 0) r += ", ";
								            r += l.get(i);
								        }
						        return r;
						    }
				
				    JSONArray actifsJson(JSONObject unite) throws Exception {
    JSONArray cands = unite.getJSONArray("candidats");
    if (!unite.has("elimines")) return cands;
    JSONArray elim = unite.getJSONArray("elimines");
    if (elim.length() == 0) return cands;
    JSONArray res = new JSONArray();
    for (int i = 0; i < cands.length(); i++) {
        boolean elimine = false;
        for (int j = 0; j < elim.length(); j++) {
            if (cands.getString(i).equals(elim.getJSONObject(j).getString("nom"))) { elimine = true; break; }
        }
        if (!elimine) res.put(cands.getString(i));
    }
    return res;
}
ArrayList<Object[]> calculerElus(JSONObject unite) throws Exception {
						        String statut = unite.getString("statut");
						        if (!statut.equals("ferme")) return null;
						        JSONArray candArr = actifsJson(unite);
						        JSONObject votesObj = unite.getJSONObject("votes");
						        int nbAdmis = unite.has("nbAdmis") ? unite.getInt("nbAdmis") : unite.getInt("nbMax");
						        if (nbAdmis > candArr.length()) nbAdmis = candArr.length();
						        if (candArr.length() == 0) return new ArrayList<Object[]>();
						
						        ArrayList<Object[]> tries = new ArrayList<Object[]>();
						        for (int i = 0; i < candArr.length(); i++) {
								            String nom = candArr.getString(i);
								            int score = votesObj.has(nom) ? votesObj.getInt(nom) : 0;
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
						
						        if (unite.has("tourSuivant")) {
								            ArrayList<Object[]> subElus = calculerElus(unite.getJSONObject("tourSuivant"));
								            if (subElus == null) return null;
								            ArrayList<Object[]> resultat = new ArrayList<Object[]>();
								            resultat.addAll(clairementElus);
								            resultat.addAll(subElus);
								            return resultat;
								        }
						        return null;
						    }
				
				    ArrayList<Object[]> resoudreRolesJson(JSONObject racine) throws Exception {
						        JSONArray candidatsOriginaux = actifsJson(racine);
						        JSONObject votesOriginaux = racine.getJSONObject("votes");
						        JSONArray roles = racine.getJSONArray("roles");
						
						        ArrayList<Object[]> pool = new ArrayList<Object[]>();
						        for (int i = 0; i < candidatsOriginaux.length(); i++) {
								            String nom = candidatsOriginaux.getString(i);
								            int score = votesOriginaux.has(nom) ? votesOriginaux.getInt(nom) : 0;
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
						        JSONObject noeudCourant = racine;
						
						        for (int r = 0; r < roles.length(); r++) {
								            JSONObject role = roles.getJSONObject(r);
								            String nomRole = role.getString("nom");
								            int n = role.getInt("nombre");
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
										                if (!noeudCourant.has("tourSuivant")) return null;
										                JSONObject departage = noeudCourant.getJSONObject("tourSuivant");
										                if (!departage.getString("statut").equals("ferme")) return null;
										
										                JSONArray depCandidats = departage.getJSONArray("candidats");
										                JSONObject depVotes = departage.getJSONObject("votes");
										                ArrayList<Object[]> depTries = new ArrayList<Object[]>();
										                for (int i = 0; i < depCandidats.length(); i++) {
												                    String nom = depCandidats.getString(i);
												                    int score = depVotes.has(nom) ? depVotes.getInt(nom) : 0;
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
										                if (depPlacesRestantes > 0 && depPlacesRestantes < depExaequo.size()) return null;
										
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
				
				    void ajouterEluJson(HashMap<String, ArrayList<String>> map, String nom, String poste) {
						        if (!map.containsKey(nom)) map.put(nom, new ArrayList<String>());
						        ArrayList<String> postes = map.get(nom);
						        if (!postes.contains(poste)) postes.add(poste);
						    }
				
				    void collecterElusUniteJson(JSONObject unite, HashMap<String, ArrayList<String>> map, String posteBase) throws Exception {
						        String statut = unite.getString("statut");
						        if (statut.equals("designe")) {
								            JSONArray designes = unite.getJSONArray("designes");
								            for (int i = 0; i < designes.length(); i++) ajouterEluJson(map, designes.getString(i), posteBase);
								        } else if (statut.equals("ferme")) {
								            if (unite.has("modeRoles") && unite.getBoolean("modeRoles")) {
										                ArrayList<Object[]> affectation = resoudreRolesJson(unite);
										                if (affectation != null) {
												                    for (int i = 0; i < affectation.size(); i++) {
														                        ajouterEluJson(map, (String) affectation.get(i)[0], (String) affectation.get(i)[2] + " " + posteBase);
														                    }
												                }
										            } else {
										                ArrayList<Object[]> elus = calculerElus(unite);
										                if (elus != null) {
												                    for (int i = 0; i < elus.size(); i++) ajouterEluJson(map, (String) elus.get(i)[0], posteBase);
												                }
										            }
								        }
						    }
				
				    HashMap<String, ArrayList<String>> calculerElusGlobalJson(JSONArray sectionsArr) throws Exception {
						        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
						        for (int i = 0; i < sectionsArr.length(); i++) {
								            JSONObject sec = sectionsArr.getJSONObject(i);
								            String titreSection = sec.getString("titre");
								            if (sec.has("sousSections")) {
										                JSONArray ss = sec.getJSONArray("sousSections");
										                for (int j = 0; j < ss.length(); j++) {
												                    JSONObject sub = ss.getJSONObject(j);
												                    collecterElusUniteJson(sub, map, sub.getString("titre") + " " + titreSection);
												                }
										            } else {
										                collecterElusUniteJson(sec.getJSONObject("unite"), map, titreSection);
										            }
								        }
						        return map;
						    }
				
				    void afficherEtat(final JSONObject etat) {
						        runOnUiThread(new Runnable() {
								            public void run() {
										                try {
												                    dernierEtat[0] = etat;
if (listeOuverte[0]) afficherListeMembres();
												                    titreProjet.setText("Projet : " + etat.getString("projet"));
												                    zoneContenu.removeAllViews();
												                    panelVoteFixe.removeAllViews();
												                    panelVoteFixe.setVisibility(View.GONE);
												                    JSONArray sectionsArr = etat.getJSONArray("sections");
												                    HashMap<String, ArrayList<String>> elusGlobal = calculerElusGlobalJson(sectionsArr);
												
												                    for (int i = 0; i < sectionsArr.length(); i++) {
														                        JSONObject sec = sectionsArr.getJSONObject(i);
														                        String titreSection = sec.getString("titre");
														                        LinearLayout sectionBox = new LinearLayout(VoteActivity.this);
														                        sectionBox.setOrientation(LinearLayout.VERTICAL);
														                        sectionBox.setPadding(25, 25, 25, 25);
														                        sectionBox.setBackgroundColor(Color.parseColor("#F0F0F0"));
														                        LinearLayout.LayoutParams lpBox = new LinearLayout.LayoutParams(
														                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
														                        lpBox.setMargins(0, 0, 0, 40);
														                        sectionBox.setLayoutParams(lpBox);
														
														                        if (sec.has("sousSections")) {
																                            TextView titreSectionTxt = new TextView(VoteActivity.this);
																                            titreSectionTxt.setText(titreSection);
																                            titreSectionTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
																                            titreSectionTxt.setTypeface(null, Typeface.BOLD);
																                            titreSectionTxt.setTextColor(Color.parseColor("#283593"));
																                            titreSectionTxt.setPadding(0, 0, 0, 10);
																                            sectionBox.addView(titreSectionTxt);
																
																                            JSONArray ssArr = sec.getJSONArray("sousSections");
																                            for (int j = 0; j < ssArr.length(); j++) {
																		                                JSONObject sub = ssArr.getJSONObject(j);
																		                                afficherBloc(sub, sectionBox, elusGlobal, sub.getString("titre") + " " + titreSection);
																		                            }
																                        } else {
																                            afficherBloc(sec.getJSONObject("unite"), sectionBox, elusGlobal, titreSection);
																                        }
														
														                        zoneContenu.addView(sectionBox);
														                    }
												                } catch (Exception e) {
												                    Toast.makeText(VoteActivity.this, "Erreur affichage", Toast.LENGTH_SHORT).show();
												                }
										            }
								        });
						    }
				
				    void afficherBloc(final JSONObject unite, LinearLayout parent, final HashMap<String, ArrayList<String>> elusGlobal, final String posteBase) throws Exception {
						        final String titre = unite.getString("titre");
						        final String statut = unite.getString("statut");
						        final int nbMax = unite.getInt("nbMax");
						        final int nbAdmis = unite.has("nbAdmis") ? unite.getInt("nbAdmis") : nbMax;
						        JSONArray candArr = unite.getJSONArray("candidats");
						        JSONObject votesObj = unite.getJSONObject("votes");
						
						        LinearLayout bloc = new LinearLayout(VoteActivity.this);
						        bloc.setOrientation(LinearLayout.VERTICAL);
						        bloc.setPadding(20, 20, 10, 20);
						
						        String badge = statut.equals("ouvert") ? "🟢 OUVERT" : statut.equals("ferme") ? "🔴 FERMÉ" : statut.equals("designe") ? "📌 DÉSIGNÉ" : "⚪ EN ATTENTE";
						        TextView titreTxt = new TextView(VoteActivity.this);
						        titreTxt.setText(badge + "  " + titre + " (max " + nbMax + " choix, " + nbAdmis + " admis)");
						        titreTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
						        titreTxt.setTypeface(null, Typeface.BOLD);
						        titreTxt.setTextColor(Color.parseColor("#303F9F"));
						        titreTxt.setPadding(0, 0, 0, 12);
						        bloc.addView(titreTxt);
						
						        if (statut.equals("designe") || statut.equals("ferme")) {
								            final LinearLayout detailsBox = new LinearLayout(VoteActivity.this);
								            detailsBox.setOrientation(LinearLayout.VERTICAL);
								            detailsBox.setVisibility(View.GONE);
								
								            final Button btnToggle = new Button(VoteActivity.this);
								            btnToggle.setText("▼ Voir les résultats");
								            btnToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
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
								            bloc.addView(btnToggle);
								
								            if (statut.equals("designe")) {
										                JSONArray designes = unite.getJSONArray("designes");
										                for (int i = 0; i < designes.length(); i++) {
												                    TextView t = new TextView(VoteActivity.this);
												                    t.setText("✓ " + designes.getString(i));
												                    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
												                    t.setTextColor(Color.parseColor("#212121"));
												                    t.setPadding(20, 6, 0, 6);
												                    detailsBox.addView(t);
												                }
										            } else if (unite.has("modeRoles") && unite.getBoolean("modeRoles")) {
										                ArrayList<Object[]> affectation = resoudreRolesJson(unite);
										                if (affectation == null) {
												                    TextView attenteTxt = new TextView(VoteActivity.this);
												                    attenteTxt.setText("🎭 Départage de poste en cours");
												                    attenteTxt.setTextColor(Color.parseColor("#CC6600"));
												                    detailsBox.addView(attenteTxt);
												                } else {
												                    for (int i = 0; i < affectation.size(); i++) {
														                        Object[] a = affectation.get(i);
														                        TextView t = new TextView(VoteActivity.this);
														                        t.setText("✓ " + (String) a[0] + " — " + (String) a[2]);
														                        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
														                        t.setTextColor(Color.parseColor("#212121"));
														                        t.setPadding(20, 6, 0, 6);
														                        detailsBox.addView(t);
														                    }
												                }
										            } else {
										                ArrayList<Object[]> elus = calculerElus(unite);
										                if (elus == null) {
												                    TextView attenteTxt = new TextView(VoteActivity.this);
												                    attenteTxt.setText("⚖️ Égalité — départage en cours");
												                    attenteTxt.setTextColor(Color.parseColor("#CC6600"));
												                    detailsBox.addView(attenteTxt);
												                } else {
												                    for (int i = 0; i < elus.size(); i++) {
														                        TextView t = new TextView(VoteActivity.this);
														                        t.setText("✓ " + (String) elus.get(i)[0]);
														                        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
														                        t.setTextColor(Color.parseColor("#212121"));
														                        t.setPadding(20, 6, 0, 6);
														                        detailsBox.addView(t);
														                    }
												                }
										            }
								            bloc.addView(detailsBox);
if (choixEnvoyes.containsKey(titre)) {
    TextView rappel = new TextView(VoteActivity.this);
    rappel.setText("🗳 Votre vote : " + joinListe(choixEnvoyes.get(titre)));
    rappel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    rappel.setTextColor(Color.parseColor("#555555"));
    rappel.setPadding(0, 6, 0, 0);
    bloc.addView(rappel);
}
								        } else if (statut.equals("attente")) {
								            TextView infoTxt = new TextView(VoteActivity.this);
								            infoTxt.setText("En attente d'ouverture");
								            infoTxt.setTextColor(Color.parseColor("#999999"));
								            bloc.addView(infoTxt);
								        } else if (statut.equals("ouvert")) {
								            if (dernierTitreVote[0].equals(titre)) {
										                LinearLayout carteVote = new LinearLayout(VoteActivity.this);
carteVote.setOrientation(LinearLayout.VERTICAL);
carteVote.setBackgroundColor(Color.parseColor("#E8F5E9"));
carteVote.setPadding(24, 18, 24, 18);
TextView envoye = new TextView(VoteActivity.this);
envoye.setText("✓ Votre vote a été envoyé");
envoye.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
envoye.setTypeface(null, Typeface.BOLD);
envoye.setTextColor(Color.parseColor("#2E7D32"));
carteVote.addView(envoye);
ArrayList<String> choixAffiches = choixEnvoyes.get(titre);
if (choixAffiches != null) {
    TextView monChoix = new TextView(VoteActivity.this);
    monChoix.setText("Votre choix : " + joinListe(choixAffiches));
    monChoix.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    monChoix.setTextColor(Color.parseColor("#1B5E20"));
    monChoix.setPadding(0, 8, 0, 4);
    carteVote.addView(monChoix);
}
TextView etatEnvoi = new TextView(VoteActivity.this);
if (confirmes.containsKey(titre)) {
    etatEnvoi.setText("✔ Reçu par l'administrateur — en attente de clôture");
    etatEnvoi.setTextColor(Color.parseColor("#2E7D32"));
} else {
    etatEnvoi.setText("⏳ En attente de confirmation du serveur…");
    etatEnvoi.setTextColor(Color.parseColor("#E65100"));
}
etatEnvoi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
carteVote.addView(etatEnvoi);
bloc.addView(carteVote);
										            } else {
										                construirePanelVote(unite, titre, nbMax, candArr, elusGlobal, posteBase);
										            }
								        }
						        parent.addView(bloc);
						
						        if (unite.has("tourSuivant")) {
								            afficherBloc(unite.getJSONObject("tourSuivant"), parent, elusGlobal, posteBase);
								        }
						    }
				
				    void construirePanelVote(final JSONObject unite, final String titre, final int nbMax, JSONArray candArr, HashMap<String, ArrayList<String>> elusGlobal, String posteBase) throws Exception {
						        panelVoteFixe.removeAllViews();
						        panelVoteFixe.setVisibility(View.VISIBLE);
						
						        TextView entete = new TextView(VoteActivity.this);
						        entete.setText("🗳 " + titre);
						        entete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
						        entete.setTypeface(null, Typeface.BOLD);
						        entete.setPadding(0, 0, 0, 8);
						        panelVoteFixe.addView(entete);
						
						        final EditText champRecherche = new EditText(VoteActivity.this);
						        champRecherche.setHint("Rechercher un nom...");
						        panelVoteFixe.addView(champRecherche);
						
						        final ScrollView scrollCases = new ScrollView(VoteActivity.this);
						        LinearLayout.LayoutParams lpScrollCases = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, 500);
						        scrollCases.setLayoutParams(lpScrollCases);
						        final LinearLayout conteneurCases = new LinearLayout(VoteActivity.this);
						        conteneurCases.setOrientation(LinearLayout.VERTICAL);
						        scrollCases.addView(conteneurCases);
						        panelVoteFixe.addView(scrollCases);
						
						        final ArrayList<CheckBox> cases = new ArrayList<CheckBox>();
						        final ArrayList<String> nomsBruts = new ArrayList<String>();
						        for (int c = 0; c < candArr.length(); c++) {
								            String nomCand = candArr.getString(c);
								            nomsBruts.add(nomCand);
								            final CheckBox cb = new CheckBox(VoteActivity.this);
								            String suffixElu = elusGlobal.containsKey(nomCand) ? "  ⭐ (" + joinListe(elusGlobal.get(nomCand)) + ")" : "";
								            cb.setText(nomCand + suffixElu);
								            if (elusGlobal.containsKey(nomCand)) cb.setTextColor(Color.parseColor("#E65100"));
								            cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
								            cb.setPadding(10, 10, 0, 10);
								            cases.add(cb);
								            ArrayList<String> selSauvee = selectionsEnCours.get(titre);
if (selSauvee != null && selSauvee.contains(nomCand)) cb.setChecked(true);
final String nomCandFinal = nomCand;
cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
    public void onCheckedChanged(CompoundButton bouton, boolean coche) {
        ArrayList<String> sel = selectionsEnCours.get(titre);
        if (sel == null) {
            sel = new ArrayList<String>();
            selectionsEnCours.put(titre, sel);
        }
        if (coche) {
            if (!sel.contains(nomCandFinal)) sel.add(nomCandFinal);
        } else {
            sel.remove(nomCandFinal);
        }
    }
});
conteneurCases.addView(cb);
								        }
						        for (int c = 0; c < cases.size(); c++) {
								            final CheckBox cb = cases.get(c);
								            cb.setOnClickListener(new View.OnClickListener() {
										                public void onClick(View v) {
												                    int total = 0;
												                    for (int k = 0; k < cases.size(); k++) if (cases.get(k).isChecked()) total++;
												                    if (total > nbMax) {
														                        cb.setChecked(false);
														                        Toast.makeText(VoteActivity.this, "Maximum " + nbMax + " choix", Toast.LENGTH_SHORT).show();
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
												                    String nomBrut = nomsBruts.get(i).toLowerCase();
												                    cases.get(i).setVisibility(filtre.isEmpty() || nomBrut.contains(filtre) ? View.VISIBLE : View.GONE);
												                }
										            }
								        });
						
						        Button btnVoter = new Button(VoteActivity.this);
						        btnVoter.setText("✅ Valider mon vote");
						        LinearLayout.LayoutParams lpBtnVoter = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpBtnVoter.setMargins(0, 10, 0, 0);
						        btnVoter.setLayoutParams(lpBtnVoter);
						        btnVoter.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                ArrayList<String> choix = new ArrayList<String>();
										                for (int k = 0; k < cases.size(); k++) if (cases.get(k).isChecked()) choix.add(nomsBruts.get(k));
										                if (choix.size() == 0) {
												                    Toast.makeText(VoteActivity.this, "Cochez au moins un candidat", Toast.LENGTH_SHORT).show();
												                    return;
												                }
										                dernierTitreVote[0] = titre;
selectionsEnCours.remove(titre);
choixEnvoyes.put(titre, new ArrayList<String>(choix));
confirmes.remove(titre);
envoyerVote(nomParticipant, choix);
if (dernierEtat[0] != null) afficherEtat(dernierEtat[0]);
										                dernierTitreVote[0] = titre;
										                if (writerRef[0] != null) {
    Toast.makeText(VoteActivity.this, "Vote envoyé", Toast.LENGTH_SHORT).show();
} else {
    Toast.makeText(VoteActivity.this, "Hors ligne : le vote sera envoyé dès la reconnexion", Toast.LENGTH_LONG).show();
}
										            }
								        });
						        panelVoteFixe.addView(btnVoter);
						    }
				
				    void afficherListeMembres() {
listeOuverte[0] = true;
						        corpsContainer.removeAllViews();
						        ScrollView scrollListe = new ScrollView(VoteActivity.this);
						        LinearLayout rootListe = new LinearLayout(VoteActivity.this);
						        rootListe.setOrientation(LinearLayout.VERTICAL);
						        rootListe.setPadding(40, 40, 40, 80);
						        rootListe.setBackgroundColor(Color.parseColor("#FFFFFF"));
						
						        TextView titre = new TextView(VoteActivity.this);
						        titre.setText("Liste des membres");
						        titre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
						        titre.setPadding(0, 0, 0, 20);
						        rootListe.addView(titre);
						
						        if (dernierEtat[0] == null) {
								            TextView attente = new TextView(VoteActivity.this);
								            attente.setText("En attente de connexion...");
								            attente.setTextColor(Color.parseColor("#888888"));
								            rootListe.addView(attente);
								        } else {
								            try {
										                JSONArray membresArr = dernierEtat[0].has("listeMembres") ? dernierEtat[0].getJSONArray("listeMembres") : new JSONArray();
										                JSONArray sectionsArr = dernierEtat[0].getJSONArray("sections");
										                HashMap<String, ArrayList<String>> elusGlobal = calculerElusGlobalJson(sectionsArr);
										
										                if (membresArr.length() == 0) {
												                    TextView vide = new TextView(VoteActivity.this);
												                    vide.setText("Aucun membre importé pour le moment.");
												                    vide.setTextColor(Color.parseColor("#888888"));
												                    rootListe.addView(vide);
												                } else {
												                    for (int i = 0; i < membresArr.length(); i++) {
														                        String nom = membresArr.getString(i);
														                        TextView ligne = new TextView(VoteActivity.this);
														                        ligne.setPadding(0, 10, 0, 10);
														                        if (elusGlobal.containsKey(nom)) {
																                            ligne.setText(nom + "  ⭐ (" + joinListe(elusGlobal.get(nom)) + ")");
																                            ligne.setTypeface(null, Typeface.BOLD);
																                            ligne.setTextColor(Color.parseColor("#E65100"));
																                        } else {
																                            ligne.setText(nom);
																                            ligne.setTextColor(Color.parseColor("#333333"));
																                        }
														                        ligne.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
														                        rootListe.addView(ligne);
														                    }
												                }
										            } catch (Exception e) {
										                TextView err = new TextView(VoteActivity.this);
										                err.setText("Erreur d'affichage");
										                rootListe.addView(err);
										            }
								        }
						
						        Button btnRetourListe = new Button(VoteActivity.this);
						        btnRetourListe.setText("⬅ Retour au vote");
						        LinearLayout.LayoutParams lpRetourListe = new LinearLayout.LayoutParams(
						            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						        lpRetourListe.setMargins(0, 20, 0, 0);
						        btnRetourListe.setLayoutParams(lpRetourListe);
						        rootListe.addView(btnRetourListe);
						        btnRetourListe.setOnClickListener(new View.OnClickListener() {
								            public void onClick(View v) {
										                listeOuverte[0] = false;
corpsContainer.removeAllViews();
										                corpsContainer.addView(scroll);
										            }
								        });
						
						        scrollListe.addView(rootListe);
						        corpsContainer.addView(scrollListe);
						    }
				
				    void envoyerVote(final String nom, final ArrayList<String> choix) {
    String msg = "";
    try {
        JSONObject obj = new JSONObject();
        obj.put("type", "vote");
        obj.put("nom", nom);
        obj.put("titre", dernierTitreVote[0]);
        JSONArray arr = new JSONArray();
        for (int i = 0; i < choix.size(); i++) arr.put(choix.get(i));
        obj.put("choix", arr);
        msg = obj.toString();
    } catch (Exception e) {
        return;
    }
    voteEnAttente[0] = msg;
    final String msgFinal = msg;
    new Thread(new Runnable() {
        public void run() {
            try {
                PrintWriter w = writerRef[0];
                if (w != null) w.println(msgFinal);
            } catch (Exception e) {}
        }
    }).start();
}
}
final Reseau reseau = new Reseau();
		
		btnConsulterListe.setOnClickListener(new View.OnClickListener() {
				    public void onClick(View v) {
						        reseau.afficherListeMembres();
						    }
		});
		
		new Thread(new Runnable() {
    public void run() {
        int tentatives = 0;
        String[] cibles = ipServeur.split(",");
int rang = 0;
while (!isFinishing()) {
            Socket socket = null;
            try {
                socket = new Socket();
                final String cible = cibles[rang++ % cibles.length].trim();
socket.connect(new java.net.InetSocketAddress(cible, 8988), 4000);
socket.setKeepAlive(true);
socket.setTcpNoDelay(true);
socket.setSoTimeout(8000);
                tentatives = 0;
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                writerRef[0] = pw;
                runOnUiThread(new Runnable() {
                    public void run() {
                        statutConnexion.setText("Connecté à " + cible);
                    }
                });
                if (voteEnAttente[0] != null) pw.println(voteEnAttente[0]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String ligne;
                while ((ligne = reader.readLine()) != null) {
socket.setSoTimeout(40000);
                    try {
                        final JSONObject obj = new JSONObject(ligne);
                        String type = obj.getString("type");
                        if (type.equals("etat")) {
                            reseau.afficherEtat(obj);
                        } else if (type.equals("ack")) {
                            String titreAck = obj.optString("titre", "");
if (titreAck.equals(dernierTitreVote[0])) voteEnAttente[0] = null;
confirmes.put(titreAck, Boolean.TRUE);
if (dernierEtat[0] != null) reseau.afficherEtat(dernierEtat[0]);
                        }
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                tentatives++;
            }
            writerRef[0] = null;
            try { if (socket != null) socket.close(); } catch (Exception e2) {}
            if (isFinishing()) return;
            final int essai = tentatives;
            runOnUiThread(new Runnable() {
                public void run() {
                    statutConnexion.setText("⚠ Connexion perdue — reconnexion automatique" + (essai > 1 ? " (essai " + essai + ")" : "") + "...");
                }
            });
            try { Thread.sleep(1500); } catch (InterruptedException ie) { return; }
        }
    }
}).start();
	}
	
	private void initializeLogic() {
	}
	
}