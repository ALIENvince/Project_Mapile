/*********************************************************************************
 * VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex (cf libClass_Projet)     *
 *       complement Ã  l'ANALYSEUR LEXICAL produit par ANTLR                      *
 *                                                                               *
 *                                                                               *
 *   nom du programme compile, sans suffixe : String UtilLex.nomSource           *
 *   ------------------------                                                    *
 *                                                                               *
 *   attributs lexicaux (selon items figurant dans la grammaire):                *
 *   ------------------                                                          *
 *     int UtilLex.valEnt = valeur du dernier nombre entier lu (item nbentier)   *
 *     int UtilLex.numIdCourant = code du dernier identificateur lu (item ident) *
 *                                                                               *
 *                                                                               *
 *   methodes utiles :                                                           *
 *   ---------------                                                             *
 *     void UtilLex.messErr(String m)  affichage de m et arret compilation       *
 *     String UtilLex.chaineIdent(int numId) delivre l'ident de codage numId     *
 *     void afftabSymb()  affiche la table des symboles                          *
 *********************************************************************************/


import java.io.*;

/**
 * classe de mise en oeuvre du compilateur
 * =======================================
 * (verifications semantiques + production du code objet)
 * 
 * @author Lohier, Faye, Jullion
 *
 */

public class PtGen {
    

    // constantes manipulees par le compilateur
    // ----------------------------------------

	private static final int 
	
	// taille max de la table des symboles
	MAXSYMB=300,

	// codes MAPILE :
	RESERVER=1,EMPILER=2,CONTENUG=3,AFFECTERG=4,OU=5,ET=6,NON=7,INF=8,
	INFEG=9,SUP=10,SUPEG=11,EG=12,DIFF=13,ADD=14,SOUS=15,MUL=16,DIV=17,
	BSIFAUX=18,BINCOND=19,LIRENT=20,LIREBOOL=21,ECRENT=22,ECRBOOL=23,
	ARRET=24,EMPILERADG=25,EMPILERADL=26,CONTENUL=27,AFFECTERL=28,
	APPEL=29,RETOUR=30,

	// codes des valeurs vrai/faux
	VRAI=1, FAUX=0,

    // types permis :
	ENT=1,BOOL=2,NEUTRE=3,

	// categories possibles des identificateurs :
	CONSTANTE=1,VARGLOBALE=2,VARLOCALE=3,PARAMFIXE=4,PARAMMOD=5,PROC=6,
	DEF=7,REF=8,PRIVEE=9,

    //valeurs possible du vecteur de translation 
    TRANSDON=1,TRANSCODE=2,REFEXT=3;


    // utilitaires de controle de type
    // -------------------------------
    /**
     * verification du type entier de l'expression en cours de compilation 
     * (arret de la compilation sinon)
     */
	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression entiere attendue");
	}
	/**
	 * verification du type booleen de l'expression en cours de compilation 
	 * (arret de la compilation sinon)
	 */
	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression booleenne attendue");
	}

    // pile pour gerer les chaines de reprise et les branchements en avant
    // -------------------------------------------------------------------

    private static TPileRep pileRep;  


    // production du code objet en memoire
    // -----------------------------------

    private static ProgObjet po;
    
    
    // COMPILATION SEPAREE 
    // -------------------
    //
    /** 
     * modification du vecteur de translation associe au code produit 
     * + incrementation attribut nbTransExt du descripteur
     *  NB: effectue uniquement si c'est une reference externe ou si on compile un module
     * @param valeur : TRANSDON, TRANSCODE ou REFEXT
     */
    private static void modifVecteurTrans(int valeur) {
		if (valeur == REFEXT || desc.getUnite().equals("module")) {
			po.vecteurTrans(valeur);
			desc.incrNbTansExt();
		}
	}    
    // descripteur associe a un programme objet (compilation separee)
    private static Descripteur desc;

     
    // autres variables fournies
    // -------------------------
    
    public static String trinome="LohierFayeJullion"; 
    
    private static int tCour; // type de l'expression compilee
    private static int vCour; // sert uniquement lors de la compilation d'une valeur (entiere ou boolenne)
  
   
    // TABLE DES SYMBOLES
    // ------------------
    //
    private static EltTabSymb[] tabSymb = new EltTabSymb[MAXSYMB + 1];
   
    // it = indice de remplissage de tabSymb
    // bc = bloc courant (=1 si le bloc courant est le programme principal)
	private static int it, bc;
	
	/** 
	 * utilitaire de recherche de l'ident courant (ayant pour code UtilLex.numIdCourant) dans tabSymb
	 * 
	 * @param borneInf : recherche de l'indice it vers borneInf (=1 si recherche dans tout tabSymb)
	 * @return : indice de l'ident courant (de code UtilLex.numIdCourant) dans tabSymb (O si absence)
	 */
	private static int presentIdent(int borneInf) {
		int i = it;
		while (i >= borneInf && tabSymb[i].code != UtilLex.numIdCourant)
			i--;
		if (i >= borneInf)
			return i;
		else
			return 0;
	}

	/**
	 * utilitaire de placement des caracteristiques d'un nouvel ident dans tabSymb
	 * 
	 * @param code : UtilLex.numIdCourant de l'ident
	 * @param cat : categorie de l'ident parmi CONSTANTE, VARGLOBALE, PROC, etc.
	 * @param type : ENT, BOOL ou NEUTRE
	 * @param info : valeur pour une constante, ad d'exÃ©cution pour une variable, etc.
	 */
	private static void placeIdent(int code, int cat, int type, int info) {
		if (it == MAXSYMB)
			UtilLex.messErr("debordement de la table des symboles");
		it = it + 1;
		tabSymb[it] = new EltTabSymb(code, cat, type, info);
	}

	/**
	 *  utilitaire d'affichage de la table des symboles
	 */
	private static void afftabSymb() { 
		System.out.println("       code           categorie      type    info");
		System.out.println("      |--------------|--------------|-------|----");
		for (int i = 1; i <= it; i++) {
			if (i == bc) {
				System.out.print("bc=");
				Ecriture.ecrireInt(i, 3);
			} else if (i == it) {
				System.out.print("it=");
				Ecriture.ecrireInt(i, 3);
			} else
				Ecriture.ecrireInt(i, 6);
			if (tabSymb[i] == null)
				System.out.println(" reference NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
		System.out.println();
	}
    
	/**
	 * Déclarations de variables globales
	 */
	private static int cptVarGlo;
	
	/*
	 * Déclarations de variables locales
	 */
	private static int cptVarLoc;
	
	/**
	 * Index d'un symbole dans tabSymb
	 */
	private static int indexSymb;
	
	/**
	 * Type de l'expression
	 */
	private static int typeIdent;
	
	/**
	 * Compteur d'expression dans un cond
	 */
	private static int cptCond;
	
	/**
	 * Nombre de parametre d'une procedure
	 */
	private static int nbParamProc;
	
	/**
	 * Nombres de paramtre mod d'une procedure
	 */
	private static int nbParamMod;
	
	/**
	 * Nombre de parametre fixe d'une procedure
	 */
	private static int nbParamFix;
	
	/**
	 * Compteur de parametre lors d'un appel
	 */
	private static int cptParamProc;
	
	/**
	 * Index d'un symbol appartenant à une procédure dans la tabSymb
	 */
	private static int indexIdent;
	
	/**
	 * Nom d'un procédure
	 */
	private static String nom;
	
	public static void initialisations() {
	
		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;
		
		cptVarGlo = 0;
		
		cptVarLoc = 0;
		
		indexSymb=0;
		
		typeIdent=0;
		
		cptCond = 0;
		
		nbParamProc = 0;

		nbParamFix = 0;

		nbParamMod = 0;
		
		cptParamProc=0;
		
		indexIdent = 0;
		
		nom="";
		
		// pile des reprises pour compilation des branchements en avant
		pileRep = new TPileRep(); 
		// programme objet = code Mapile de l'unite en cours de compilation
		po = new ProgObjet();
		// COMPILATION SEPAREE: desripteur de l'unite en cours de compilation
		desc = new Descripteur();
		
		// initialisation necessaire aux attributs lexicaux
		UtilLex.initialisation();
	
		// initialisation du type de l'expression courante
		tCour = NEUTRE;

	} // initialisations

	
	/**
	 *  code des points de generation A COMPLETER
	 *  -----------------------------------------
	 * @param numGen : numero du point de generation a executer
	 */
	public static void pt(int numGen) {
		switch (numGen) {
		case 0:
			initialisations();
			break;
			
		/*
		 * Initialisation/Arret du programme
		 */
			
		case 11:
			desc = new Descripteur();
			desc.setUnite("programme");
			break;
			
		case 12:
			po.produire(ARRET);
			desc.setTailleCode(po.getIpo());
			break;
			
		/*
		 * Initialisation du module
		 */
			
		case 21:
			desc = new Descripteur();
			desc.setUnite("module");
			break;
			
		case 22:
			desc.setTailleCode(po.getIpo());
			break;
			
		/*
		 * Déclarations des procédures DEF
		 */
			
		case 41:
			nom = UtilLex.chaineIdent(UtilLex.numIdCourant);
			/* Verification si la procédure n'est pas déja présente dans tabDef */
			if(desc.presentDef(nom) == 0) {
				desc.ajoutDef(nom);
			} else {
				UtilLex.messErr(nom + " déja présent dans la table des procédures définies en DEF");
			}
			break;
			
		/**
		 * Mise à jour du nombre de parametre d'une ref dans tabSymb et tabRef
		 */
		case 51:
			tabSymb[bc].info = nbParamProc;
			desc.modifRefNbParam(desc.getNbRef(), nbParamProc);
			bc=1;
			break;
			
		/**
		 * Déclarations des procédures REF
		 */
		case 61:
			indexSymb = presentIdent(UtilLex.numIdCourant);
			if(indexSymb < 1) {
				nbParamProc = 0;
				/* Ajout de la procedure dans tabRef et TabSymb*/
				desc.ajoutRef(UtilLex.chaineIdent(UtilLex.numIdCourant));
				placeIdent(UtilLex.numIdCourant,PROC,NEUTRE,desc.getNbRef());
				placeIdent(-1,REF,NEUTRE,0);
				/*On utilise bc pour sauvegarder le numéro de ligne à modifier*/
				bc = it;
			} else {
				UtilLex.messErr(UtilLex.chaineIdent(UtilLex.numIdCourant) + " est déja présent dans la table des références");
			}
			break;
		
		case 62:
			/* Ajout d'un PARAMFIX d'une ref dans tabSymb */
			placeIdent(-1,PARAMFIXE,tCour,-1);
			nbParamProc++;
			break;
		
		case 63:
			/* Ajout d'un PARAMMOD d'une ref dans tabSymb */
			placeIdent(-1,PARAMMOD,tCour,-1);
			nbParamProc++;
			break;
			
		/*
		 * Déclaration de constante
		 */
		case 71:
			indexSymb = presentIdent(bc);
			/* On verifie que la constante n'est pas dans la table des Symboles */
			if (indexSymb < 1) {
			    placeIdent(UtilLex.numIdCourant, CONSTANTE, tCour, vCour);
			}
			else {
				UtilLex.messErr("La constante ' " + UtilLex.chaineIdent(UtilLex.numIdCourant) +" ' est deja declaree" );
			}
			break;
		
		case 81:
			indexSymb = presentIdent(bc);
			/* On verifie que la constante n'est pas dans la table des Symboles */
			if(indexSymb < 1){
				/* Si on est dans une procedure, on la rajoute dans tabSymb telle une variable locale */
			    if(bc > 1) {
			    	placeIdent(UtilLex.numIdCourant,VARLOCALE,tCour,cptVarLoc+nbParamProc+2);
			    	cptVarLoc++;
			    } else {
			    	placeIdent(UtilLex.numIdCourant, VARGLOBALE, tCour, cptVarGlo);
			    	cptVarGlo++;
			    }
			}
			else {
			    UtilLex.messErr("La variable ' " + UtilLex.chaineIdent(UtilLex.numIdCourant) + " ' est deja declaree" );
			}
			break;
		case 82:
			/* On reserve le nombre de variables vus lors du rajout dans tabSymb, selon la valeur du bloc courant
			   On reserve seulement si l'unite est un programme, on rajoute aussi le nombre de variables globales dans le descripteur */
			if(desc.getUnite() == "programme") {
				if(bc>1) {
					po.produire(RESERVER);
					po.produire(cptVarLoc);
				} else {
					po.produire(RESERVER);
					po.produire(cptVarGlo);
				}
			}
			desc.setTailleGlobaux(cptVarGlo);
			break;
			
		/*
		 * Déclaration du type
		 */
		case 91:
			tCour = ENT;
			break;
		case 92:
			tCour = BOOL;
			break;

		/**
		 * Gestion de toutes les procédures
		 */
			
		case 101:
			if(desc.getUnite().equals("programme")) {
				/*On place un bincond avant la déclaration des proc */
				po.produire(BINCOND);
				po.produire(0);
				pileRep.empiler(po.getIpo());
			}
			break;
			
		case 102:
			if(desc.getUnite().equals("programme")) {
				/* On modifie le bincond pour définir la ligne de retour après la déclaration */
				po.modifier(pileRep.depiler(),po.getIpo()+1);
			}
			break;
			
		/*
		 * Déclaration d'une procedure
		 */
		case 111:
			indexSymb = presentIdent(1);
			if(indexSymb < 1) {
				/* Si le nom de la procédure n'est pas dans tabSymb, on rajoute dans tabSymb et on définit le bloc courant*/
				nbParamProc = 0;
				nom = UtilLex.chaineIdent(UtilLex.numIdCourant);
				/* On vérifie si la procédure est dans tabDef sinon c'est une procédure privée*/
				if(desc.presentDef(nom) == 0) {
					placeIdent(UtilLex.numIdCourant,PROC,NEUTRE,po.getIpo()+1);
					placeIdent(-1,PRIVEE,NEUTRE,0);
				} else {
					placeIdent(UtilLex.numIdCourant,PROC,NEUTRE,po.getIpo()+1);
					placeIdent(-1,DEF,NEUTRE,0);
					desc.modifDefAdPo(desc.presentDef(nom), po.getIpo()+1);
				}
				bc=it+1;
			} else {
				UtilLex.messErr("Nom de processus " + UtilLex.chaineIdent(UtilLex.numIdCourant) + " deja présent dans la table des symboles");
			}
			break;
			
		case 112:
			/*On met a jour le nombre de parametres de la procédure dans tabSymb et tabDef si elle est présente*/
			tabSymb[bc-1].info = nbParamProc;
			if(desc.presentDef(nom) != 0) {
				desc.modifDefNbParam(desc.presentDef(nom), nbParamProc);
			}
			cptVarLoc = 0;
			break;
			
		/* MAJ de la table des Symboles  */
		case 113:
			/* Suppresion des variables locales */
			it-=cptVarLoc;
			cptVarLoc=0;
			/* Masquage des parametres */
			for(int i=it;i>(it-nbParamProc);i--) {
				tabSymb[i].code = (-1);
			}
			/* Mise a jour de bc */
			bc=1;
			
			po.produire(RETOUR);
			po.produire(nbParamProc);
			break;
			
		/*
		 * pf
		 */
			
		case 151:
			indexSymb = presentIdent(bc);
			if(indexSymb < 1) {
				placeIdent(UtilLex.numIdCourant,PARAMFIXE,tCour,nbParamProc);
				nbParamProc++;
			} else {
				UtilLex.messErr(UtilLex.chaineIdent(UtilLex.numIdCourant) + "est deja dans la table des symboles");
			}
			break;
			
		/*
		 * pm
		 */
			
		case 171:
			indexSymb = presentIdent(bc);
			if(indexSymb < 1) {
				placeIdent(UtilLex.numIdCourant,PARAMMOD,tCour,nbParamProc);
				nbParamProc++;
			} else {
				UtilLex.messErr(UtilLex.chaineIdent(UtilLex.numIdCourant) + "est deja dans la table des symboles");
			}
			break;
			
		/*
		 * Boucle if
		 */
			
		case 201:
			if(tCour == BOOL) {
				po.produire(BSIFAUX);
				po.produire(0);
				/* Ajout d'une ligne au vecteur de translation */
				modifVecteurTrans(TRANSCODE);
				/* On retient le trou du BSIFAUX */
				pileRep.empiler(po.getIpo());
			} else {
				UtilLex.messErr("Expression de si invalide");
			}
			break;
			
		 case 202:
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(0);
			/* Ajout d'une ligne au vecteur de translation */
			modifVecteurTrans(TRANSCODE);
			/* On retient le trou du BINCOND */
			pileRep.empiler(po.getIpo());
			break;
			
		case 203:
			/* On modifie le trou du BSIFAUX pour rédiriger apres le fsi */
			po.modifier(pileRep.depiler(), po.getIpo()+1);
			break;
			
		/*
		 * Boucle cond
		 */
		case 211:
			pileRep.empiler(0);
			break;
			
		case 212:
			po.produire(BSIFAUX);
			po.produire(0);
			/* Ajout d'une ligne au vecteur de translation */
			modifVecteurTrans(TRANSCODE);
			/* On retient le trou du BSIFAUX */
			pileRep.empiler(po.getIpo());
			break;
			
		case 213:
			/* MAJ du bsifaux et depile la deuxieme valeur dans la pile */
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(pileRep.depiler());
			/* Ajout d'une ligne au vecteur de translation */
			modifVecteurTrans(TRANSCODE);
			/* On retient le trou du bincond */
			pileRep.empiler(po.getIpo());
			break;
			
		case 214:
			/* Dans le cas ou il n'y pas de aut, on redirige le dernier BSIFAUX à la fin du fcond */
			po.modifier(pileRep.depiler(), po.getIpo()+1);
			break;
			
		case 215: 
			/* On remonte la pile de reprise */
			cptCond=pileRep.depiler();
			while(po.getElt(cptCond) != 0) {
				int tmp = po.getElt(cptCond);
				po.modifier(cptCond, po.getIpo()+1);
				cptCond=tmp;
			}
			po.modifier(cptCond, po.getIpo()+1);
			break;
			
		/*
		 * Boucle ttq	
		 */
			
		/* On retient le numéro de ligne de début de l'expression  */
		case 221:
			pileRep.empiler(po.getIpo()+1);
			break;
			
		case 222:
			/* On vérifie si la condition est de type bool */
			if(tCour == BOOL ) { 
				po.produire(BSIFAUX);
				po.produire(0);
				/* Ajout d'une ligne au vecteur de translation */
				modifVecteurTrans(TRANSCODE);
				/* On retient le trou du BSIFAUX */
				pileRep.empiler(po.getIpo());
			} else {
				UtilLex.messErr("Expression du ttq invalide");
			}
			break;
			
		case 223: 
			/* On modifie le bisfaux qui nous permet de sortir de la boucle si la condition est fausse, sinon on remonte vers la condition avec un bincond */
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(pileRep.depiler());
			/* Ajout d'une ligne au vecteur de translation */
			modifVecteurTrans(TRANSCODE);
			break;
			
		/*
		 * Lecture/Ecriture
		 */
		
		/* Lecture */	
		case 231:
			indexSymb = presentIdent(1);
			if(indexSymb != 0) {
				EltTabSymb row = tabSymb[indexSymb];
				int type = row.type;
				switch(row.categorie) {
					case VARGLOBALE:
						if(type == BOOL) {
							po.produire(LIREBOOL);
						} else {
							po.produire(LIRENT);
						}
						po.produire(AFFECTERG);
						po.produire(tabSymb[indexSymb].info);
						/* Ajout d'une ligne au vecteur de translation */
						modifVecteurTrans(TRANSDON);
						break;
					case VARLOCALE:
						if(type == BOOL) {
							po.produire(LIREBOOL);
						} else {
							po.produire(LIRENT);
						}
						po.produire(AFFECTERL);
					    po.produire(tabSymb[indexSymb].info);
					    po.produire(0);
						break;
					case PARAMMOD:
						if(type == BOOL) {
							po.produire(LIREBOOL);
						} else {
							po.produire(LIRENT);
						}
						po.produire(AFFECTERL);
					    po.produire(tabSymb[indexSymb].info);
					    po.produire(1);
						break;
					default:
						UtilLex.messErr("Le type de ' "+ UtilLex.chaineIdent(UtilLex.numIdCourant) + " ' ne permet pas l'écriture");
						break;
				}
			} else {
				UtilLex.messErr( "' " +UtilLex.chaineIdent(UtilLex.numIdCourant) + " ' n'est pas dans la table des symboles");
			}
			break;
			
		/* Ecriture */	
		case 241:
			if(tCour==BOOL) {
				po.produire(ECRBOOL);
			} else if (tCour==ENT) {
				po.produire(ECRENT);
			} else {
				UtilLex.messErr("Type d'expression non connu");
			}
			break;
			
		/*
		 * Affectation ou appel
		 */
			
		case 251:
			/*Recuperation du type de l'ident, si il est present dans la table des symboles	*/
			indexSymb = presentIdent(1);
			if(indexSymb != 0) {
				EltTabSymb row = tabSymb[indexSymb];
				int type = row.type;
				if(type == BOOL) {
				    typeIdent=BOOL;
				} else {
				    typeIdent=ENT;
				}
			} else {
				UtilLex.messErr(UtilLex.chaineIdent(UtilLex.numIdCourant) + " n'est pas dans la table des symboles");
			}
			break;
			
		case 252:
			/* Verification de la concordance entre le type de l'ident et de l'expression que l'on lui affecte */
			if(typeIdent == tCour) { 
				EltTabSymb row = tabSymb[indexSymb];
				switch(row.categorie) {
					case VARGLOBALE:
						po.produire(AFFECTERG);
						po.produire(row.info);
					    /* Ajout d'une ligne au vecteur de translation */
						modifVecteurTrans(TRANSDON);
						break;
					case PARAMMOD:
						po.produire(AFFECTERL);
						po.produire(row.info);
						po.produire(1);
						break;
					case VARLOCALE:
						po.produire(AFFECTERL);
						po.produire(row.info);
						po.produire(0);
						break;
					default :
						UtilLex.messErr("Type de " + UtilLex.chaineIdent(UtilLex.numIdCourant) + " non compatible avec l'affectation");
						break;
				}
			} else {
				UtilLex.messErr("Le type de l'ident et de l'expression sont incompatibles");
			}
			break;
			
		//Récupération du nombres de paramfixe et paramod
		case 253:
			int i = indexSymb+2;
			while(tabSymb[i].categorie == PARAMFIXE) {
				nbParamFix++;
				i++;
			}
			while(tabSymb[i].categorie == PARAMMOD) {
				nbParamMod++;
				i++;
			}
			break;
		
		//Verification du nombre de param fix lors de l'appel d'une proc
		case 254:
			if(nbParamFix != cptParamProc) {
				UtilLex.messErr("Nombre de parametres fixe incorrect");
			}
			break;
			
		//Verification du nombre de param mod lors de l'appel d'une proc
		case 255:
			if(nbParamMod != (cptParamProc-nbParamFix) ) {
				UtilLex.messErr("Nombre de parametres mod incorrect");
			} else {
				cptParamProc=0;
				nbParamFix=0;
				nbParamMod=0;
			}
			break;
			
		//Appel
		case 256:
			po.produire(APPEL);
			po.produire(tabSymb[indexSymb].info);
			if(tabSymb[indexSymb+1].categorie == REF) {
				/* Ajout d'une ligne au vecteur de translation */
				modifVecteurTrans(REFEXT);
			}
			po.produire(tabSymb[indexSymb+1].info);
			break;
			
		/*
		 * Appel des parametres Fixes	
		 */
		case 261:
			if(tabSymb[indexSymb+2+cptParamProc].type == tCour) {
				cptParamProc++;
			} else {
				UtilLex.messErr("Type de l'expression différent du type du parametre");
			}
			break;
			
		/*
		 * Appel des parametres Mod
		 */
		case 271:
			indexIdent = presentIdent(UtilLex.numIdCourant);
			if(tabSymb[indexSymb+2+cptParamProc].type == tabSymb[indexIdent].type) {
				if(indexIdent != 0) {
					EltTabSymb row = tabSymb[indexIdent];
					switch(row.categorie) {
						case VARGLOBALE:
							po.produire(EMPILERADG);
							po.produire(row.info);
						    /* Ajout d'une ligne au vecteur de translation */
							modifVecteurTrans(TRANSDON);
							cptParamProc++;
							break;
						case VARLOCALE:
							po.produire(EMPILERADL);
							po.produire(row.info);
							po.produire(0);
							cptParamProc++;
							break;
						case PARAMMOD:
							po.produire(EMPILERADL);
							po.produire(row.info);
							po.produire(1);
							cptParamProc++;
							break;
						default:
							UtilLex.messErr("Type non compatible en parametre mod");
							break;
					}
				} else {
					UtilLex.messErr("Ident non présent dans la table");
				}
			} else {
				UtilLex.messErr("Type de l'expression différent du type du parametre");
			}
			break;
			
		/*
		 * Expression OU
		 */
			
		/* exp1 ou exp2 */
		case 281:
			po.produire(OU);
			tCour=BOOL;
			break;
			
		/*
		 * Expression ET
		 */
			
		/* exp1 et exp2 */
		case 291:
			po.produire(ET);
			tCour=BOOL;
			break;
			
		/*
		 * Expression NON
		 */
			
		/* non exp */
		case 301:
			po.produire(NON);
			tCour=BOOL;
			break;
			
		/*
		 * Expressions EG/DIFF/SUP/SUPEG/INF/INFEG
		 */
			
		/* '=' */
		case 311:
			po.produire(EG);
			tCour=BOOL;
			break;
			
		/* '<>' */
		case 312:
			po.produire(DIFF);
			tCour=BOOL;
			break;
			
		/* '>' */
		case 313:
			po.produire(SUP);
			tCour=BOOL;
			break;
			
		/* '>=' */
		case 314:
			po.produire(SUPEG);
			tCour=BOOL;
			break;
			
		/* '<' */
		case 315:
			po.produire(INF);
			tCour=BOOL;
			break;
			
		/* '<=' */
		case 316:
			po.produire(INFEG);
			tCour=BOOL;
			break;
		
		/*
		 * Expressions + et -
		 */
		
		/* '+' */
		case 321:
			po.produire(ADD);
			tCour=ENT;
			break;
			
		/* '-' */
		case 322:
			po.produire(SOUS);
			tCour=ENT;
			break;
			
		/*
		 * Expressions * et div
		 */
			
		/* '*' */
		case 331:
			po.produire(MUL);
			tCour=ENT;
			break;
			
		/* 'div' */
		case 332:
			po.produire(DIV);
			tCour=ENT;
			break;
			
		/*
		 * Definition d'un type primaire
		 */
		
		/* Valeur*/
		case 341: 
			po.produire(EMPILER);
			po.produire(vCour);
			break;
			
		/* Ident */
		case 342:
			int k = presentIdent(1);
			if(k > 0) {
				switch(tabSymb[k].categorie) {
					case CONSTANTE:
						po.produire( EMPILER );
						po.produire( tabSymb[k].info );
						break;
					case VARGLOBALE:
						po.produire ( CONTENUG );
						po.produire( tabSymb[k].info  );
					    /* Ajout d'une ligne au vecteur de translation */
						modifVecteurTrans(TRANSDON);
						break;
					case VARLOCALE:
					case PARAMFIXE:
						po.produire( CONTENUL );
						po.produire(tabSymb[k].info);
						po.produire(0);
						break;
					case PARAMMOD:
						po.produire( CONTENUL );
						po.produire(tabSymb[k].info);
						po.produire(1);
						break;
					default : UtilLex.messErr("Type non reconnu");
							  break;
				}
				tCour = tabSymb[k].type;
			} else {
				UtilLex.messErr("Ident non déclarée");
			}
			break;
			
		/*
		 * Définition des différentes valeurs
		 */
		
		/* Entier positif */
		case 351:
			tCour = ENT;
			vCour = UtilLex.valEnt;
			break;
			
		/* Entier négatif */
		case 352:
			tCour = ENT;
			vCour = - UtilLex.valEnt;
			break;
			
		/* Booléen vrai */
		case 353:
			tCour = BOOL;
			vCour = VRAI;
			break;
			
		/* Booléan true */
		case 354:
			tCour = BOOL;
			vCour = FAUX;
			break;
			
		/*
		 * Affichage de la table des symboles
		 */
		case 400:
			for(int j = 1; j <= desc.getNbDef(); j++ ) {
                if(desc.getDefAdPo(j) == -1) {
                    UtilLex.messErr("La procédure " +desc.getDefNomProc(j) + " est déclarée au début du programme mais n'est pas été définie.");
                }
            }
			afftabSymb();
			po.constObj();
			po.constGen();
			desc.ecrireDesc(UtilLex.nomSource);
			break;
			
		/*
		 * Verification du type Entier
		 */
			
		case 401:
			verifEnt();
			break;
			
		/*
		 * Verification du type Booléen
		 */
			
		case 402:
			verifBool();
			break;
		/*
		 * Point de génération non reconnu	
		 */
		default:
			System.out.println("Point de generation non prevu dans votre liste");
			break;
		}
	}
}