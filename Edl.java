import java.io.*;
import java.util.HashMap;
import java.util.Map;

 /**
 * 
 * @author  FAYE,LOHIER,JULLION
 * @version 2020
 *
 */


public class Edl {

	// nombre max de modules, taille max d'un code objet d'une unite
	static final int MAXMOD = 5, MAXOBJ = 1000;
	// nombres max de references externes (REF) et de points d'entree (DEF)
	// pour une unite
	private static final int MAXREF = 10, MAXDEF = 10;

	// typologie des erreurs
	private static final int FATALE = 0, NONFATALE = 1;

	// valeurs possibles du vecteur de translation
	private static final int TRANSDON=1,TRANSCODE=2,REFEXT=3;

	// table de tous les descripteurs concernes par l'edl
	static Descripteur[] tabDesc = new Descripteur[MAXMOD + 1];

	static int ipo, nMod, nbErr;
	static String nomProg;
	private static String[] nomFichier = new String[MAXMOD+1];
	private static int[] transDon = new int[MAXMOD+1];
	private static int[] transCode = new int[MAXMOD+1];
	private static HashMap<String,Pair> dicoDef = new HashMap<String,Pair>((MAXMOD + 1) * MAXDEF);
	private static int[][] adFinale = new int[MAXMOD+1][MAXREF];


	// utilitaire de traitement des erreurs
	// ------------------------------------
	static void erreur(int te, String m) {
		System.out.println(m);
		if (te == FATALE) {
			System.out.println("ABANDON DE L'EDITION DE LIENS");
			System.exit(1);
		}
		nbErr = nbErr + 1;
	}

	// utilitaire de remplissage de la table des descripteurs tabDesc
	// --------------------------------------------------------------
	static void lireDescripteurs() {
		String s;
		System.out.println("les noms doivent etre fournis sans suffixe");
		System.out.print("nom du programme : ");
		s = Lecture.lireString();
		tabDesc[0] = new Descripteur();
		tabDesc[0].lireDesc(s);
		if (!tabDesc[0].getUnite().equals("programme"))
			erreur(FATALE, "programme attendu");
		nomProg = s;
		/* On stocke le nom du fichier programme */
		nomFichier[0] = s;
		nMod = 0;
		while (!s.equals("") && nMod < MAXMOD) {
			System.out.print("nom de module " + (nMod + 1)
					+ " (RC si termine) ");
			s = Lecture.lireString();
			if (!s.equals("")) {
				nMod = nMod + 1;
				tabDesc[nMod] = new Descripteur();
				tabDesc[nMod].lireDesc(s);

				/* On stocke le nom du fichier module*/
				nomFichier[nMod]=s;
				
				if (!tabDesc[nMod].getUnite().equals("module"))
					erreur(FATALE, "module attendu");
			}
		}
	}


	static void constMap() {
		// f2 = fichier executable .map construit
		OutputStream f2 = Ecriture.ouvrir(nomProg + ".map");
		if (f2 == null)
			erreur(FATALE, "creation du fichier " + nomProg
					+ ".map impossible");
		// pour construire le code concatene de toutes les unités
		int[] po = new int[(nMod + 1) * MAXOBJ + 1];
		
		/* Boucle qui parcours tout les fichiers */
		for(int i = 0;i<=nMod;i++) {
			/* On recupere le ieme fichier et on l'ouvre pour la lecture */
			InputStream unit = Lecture.ouvrir(nomFichier[i] + ".obj");
			/* Création d'une HashMap et remplissage de celle avec la ligne de le type de modification*/
			HashMap<Integer,Integer> trans = new HashMap<Integer,Integer>();
			for(int j=1;j<=tabDesc[i].getNbTransExt();j++) {
				int adresse = Lecture.lireInt(unit);
				int type = Lecture.lireInt(unit);
				trans.put(adresse, type);
			}
			
			/* On gere le cas ou il n'y a pas de RESERVER dans le programme mais qu'il y a des variables a reserver dans les modules */
			if(i==0 && tabDesc[0].getTailleGlobaux()==0 && transDon[nMod] != 0) {
				po[1]=1;
				po[2]=0;
				ipo=2;
			}
			
			
			/* Parcours des ligne de chaque fichier a partir de la ligne apres TransExt jusque la fin + 1*/
			for(int k=tabDesc[i].getNbTransExt()+1; k<tabDesc[i].getTailleCode()+tabDesc[i].getNbTransExt()+1;k++) {
				/* On recupère l'element courant du .obj */
				int elem = Lecture.lireInt(unit);
				
				/* On verifie si la ligne est presente dans trans */
				int ligne = k - (tabDesc[i].getNbTransExt());
				/* Si oui, on effectue la modification correpondante */
				if(trans.containsKey(ligne)) {
					switch(trans.get(ligne)) {
					case TRANSDON: 
						elem += transDon[i];
						break;
					case TRANSCODE:
						elem += transCode[i];
						break;
					case REFEXT:
						elem = adFinale[i][elem];
						break;
					default:
						erreur(FATALE,"Erreur sur le type de translation");
					}
				}
				/* On augmente ipo et on affecte a cet indice notre element du .obj */
				ipo++;
				po[ipo] = elem;
			}
			Lecture.fermer(unit);
		}
		/* On définit le nombre de variable globales totales */
		po[2] = transDon[nMod]+tabDesc[nMod].getTailleGlobaux();
		/* On écrit dans notre .map */
		for(int i=0;i<ipo;i++) {
			Ecriture.ecrireStringln(f2, ""+po[i]);
		}
		
		Ecriture.fermer(f2);

		// creation du fichier en mnemonique correspondant
		Mnemo.creerFichier(ipo, po, nomProg + ".ima");
	}

	/*
	 * Fonction rajoutée pour vérifier le bon remplissage des différentes tables
	 */
	private static void printTables() {
		String don = "TransDon :";
		String code = "TransCode :";
		for(int i = 0;i <nMod+1;i++) {
			don += " " + transDon[i] +" |"; 
			code += " " + transCode[i] + " |";
		}
		System.out.println(don);
		System.out.println(code);
		System.out.println("dicoDef :");
		for(Map.Entry<String, Pair> entry : dicoDef.entrySet()) {
			String s = entry.getKey();
			Pair p = entry.getValue();
			System.out.println(s + " | " + p.adPo + " | " + p.nbParam);
		}
		
		for(int i=0;i<=nMod;i++) {
			System.out.println("[ " + i + " ] ->");
			for(int j=1;j<=tabDesc[i].getNbRef();j++) {
				System.out.print(" { " + adFinale[i][j] + " }");
			}
		}
	}
	
	public static void main(String argv[]) {
		System.out.println("EDITEUR DE LIENS / PROJET LICENCE");
		System.out.println("---------------------------------");
		System.out.println("");
		nbErr = 0;
		
		
		// Phase 1 de l'edition de liens
		// -----------------------------
		lireDescripteurs();

		/* Construction de transDon et transCode */
		transDon[0] = 0;
		transCode[0] = 0;
		for(int i = 1; i<nMod+1;i++) {
			transDon[i] = transDon[i-1] + tabDesc[i-1].getTailleGlobaux();
			transCode[i] = transCode[i-1] + tabDesc[i-1].getTailleCode();
		}
		
		/* Construction DicoDef */
		for(int i=0;i<=nMod;i++) {
			for(int j=1;j<=tabDesc[i].getNbDef();j++) {
				String defNomProc = tabDesc[i].getDefNomProc(j);
				if(!dicoDef.containsKey(defNomProc)) {
					dicoDef.put(defNomProc, new Pair(tabDesc[i].getDefAdPo(j) + transCode[i],tabDesc[i].getDefNbParam(j)));
				} else {
					erreur(NONFATALE,"Double Déclaration de " + defNomProc);
				}
			}
		}
		
		/* Construction adFinale */
		for(int i=0;i<=nMod;i++) {
			for(int j=1;j<=tabDesc[i].getNbRef();j++) {
				String refNomProc = tabDesc[i].getRefNomProc(j);
				if(dicoDef.containsKey(refNomProc)) {
					if(dicoDef.get(refNomProc).nbParam == tabDesc[i].getRefNbParam(j)) {
						adFinale[i][j] = dicoDef.get(refNomProc).adPo;
					} else {
						erreur(NONFATALE,"Nombre de parametres différents");
					}
				} else {
					erreur(NONFATALE,tabDesc[i].getRefNomProc(j)+ " ne figure pas dans dicoDef");
				}
			}
		}

		if (nbErr > 0) {
			System.out.println("programme executable non produit");
			System.exit(1);
		}

		
		// Phase 2 de l'edition de liens
		// -----------------------------
		constMap();
		System.out.println("Edition de liens terminee");
	}
}

/*
 * Classe privé permettant de créer une paire pour le dicoDef
 */
class Pair {
	int adPo;
	int nbParam;
	public Pair(int x,int y) {
		adPo=x;
		nbParam=y;
	}
	public Pair() {
		adPo=0;
		nbParam=0;
	}
}
