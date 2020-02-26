/*********************************************************************************
 * VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex (cf libClass_Projet)     *
 *       complement √† l'ANALYSEUR LEXICAL produit par ANTLR                      *
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
    
    public static String trinome="Lohier_Faye_Jullion"; 
    
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
	 * @param info : valeur pour une constante, ad d'ex√©cution pour une variable, etc.
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
	 * DÈclarations de variables utiles
	 */
	private static int cptVar;
	
	/**
	 * Index d'un symbole dans tabSymb
	 */
	private static int indexSymb;
	/**
	 * Type de l'expression
	 */
	private static int typeExpr;
	/**
	 *  initialisations A COMPLETER SI BESOIN
	 *  -------------------------------------
	 */
	public static void initialisations() {
	
		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;
		
		// Compteur de variables/constante
		cptVar = 0;
		
		indexSymb=0;
		
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
		System.out.println(numGen);
		switch (numGen) {
		case 0:
			initialisations();
			break;
		/*
		 * DÈclaration de constante
		 */
		case 71:
			indexSymb = presentIdent(UtilLex.numIdCourant);
			if (indexSymb < 1) {
			    placeIdent(UtilLex.numIdCourant, CONSTANTE, tCour, vCour);
			}
			else {
			    System.out.println("Constante deja declaree");
			}
			break;
		
		/*
		 * DÈclaration de variable
		 */
		case 81:
			indexSymb = presentIdent(UtilLex.numIdCourant);
			if(indexSymb < 1){
			    placeIdent(UtilLex.numIdCourant, VARGLOBALE, tCour, cptVar);
			    cptVar++;
			}
			else {
			    System.out.println("La variable : "  + " est deja declaree" );
			}
			break;
		case 82:
			po.produire(RESERVER);
			po.produire(cptVar);
			break;
		/*
		 * DÈclaration du type
		 */
		case 91:
			tCour = ENT;
			break;

		case 92:
			tCour = BOOL;
			break;

		/*
		 * Lecture/Ecriture
		 */
		case 231: //Lecture
			indexSymb = presentIdent(UtilLex.numIdCourant);
			if(indexSymb != 0) {
				EltTabSymb row = tabSymb[indexSymb];
				if(row.categorie == VARGLOBALE) {
					int type = row.type;
					if(type == BOOL) {
						po.produire(LIREBOOL);
					    po.produire(AFFECTERG);
					    po.produire(tabSymb[indexSymb].info);
					    tCour = BOOL;
					} else {
						po.produire(LIRENT);
					    po.produire(AFFECTERG);
					    po.produire(tabSymb[indexSymb].info);
					    tCour=ENT;
					}
				} else {
					UtilLex.messErr("Le type de "+ row.code + " ne permet pas l'Ècriture");
				}
			} else {
				UtilLex.messErr(UtilLex.numIdCourant + " n'est pas dans la table des symboles");
			}
			break;
		case 241: //Ecriture
			indexSymb = presentIdent(UtilLex.numIdCourant);
			if(indexSymb != 0) {
				EltTabSymb row = tabSymb[indexSymb];
					int type = row.type;
					if(type == BOOL) {
						po.produire(CONTENUG);
					    po.produire(row.info);
					    po.produire(ECRBOOL);
					} else {
						po.produire(CONTENUG);
					    po.produire(row.info);
					    po.produire(ECRENT);
					}
			} else {
				UtilLex.messErr(UtilLex.numIdCourant + " n'est pas dans la table des symboles");
			}
			break;
			
		/*
		 * Affectation ou appel
		 */
		case 251:
			indexSymb = presentIdent(UtilLex.numIdCourant);
			if(indexSymb != 0) {
				EltTabSymb row = tabSymb[indexSymb];
				int type = row.type;
				if(type == BOOL) {
					po.produire(CONTENUG);
				    po.produire(row.info);
				    typeExpr=BOOL;
				} else {
					po.produire(CONTENUG);
				    po.produire(row.info);
				    typeExpr=ENT;
				}
			} else {
				UtilLex.messErr(UtilLex.numIdCourant + " n'est pas dans la table des symboles");
			}
			break;
		case 252:
			if(typeExpr == tCour) {
				po.produire(AFFECTERG);
				po.produire(indexSymb);
			} else {
				UtilLex.messErr("Le type de l'ident et de l'expression sont incompatibles");
			}
			break;
		/*
		 * Expression OU
		 */
		case 281:
			verifBool();
			break;
		case 282:
			po.produire(OU);
			break;
		/*
		 * Expression ET
		 */
		case 291:
			verifBool();
			break;
		case 292:
			po.produire(ET);
			break;
		/*
		 * Expression NON
		 */
		case 301:
			verifBool();
			break;
		case 302:
			po.produire(NON);
			break;
		/*
		 * Expressions EG/DIFF/SUP/SUPEG/INF/INFEG
		 */
		case 311:
			verifEnt();
			break;
		case 312:
			po.produire(EG);
			break;
		case 313:
			po.produire(DIFF);
			break;
		case 314:
			po.produire(SUP);
			break;
		case 315:
			po.produire(SUPEG);
			break;
		case 316:
			po.produire(INF);
			break;
		case 317:
			po.produire(INFEG);
			break;
		/*
		 * Expressions + et -
		 */
		case 321:
			verifEnt();
			break;
		case 322:
			po.produire(ADD);
			break;
		case 323:
			po.produire(SOUS);
			break;
			
		/*
		 * Expressions * et div
		 */
		case 331:
			verifEnt();
			break;
		case 332:
			po.produire(MUL);
			break;
		case 333:
			po.produire(DIV);
			break;
		/*
		 * 
		 */
		case 341:
			po.produire(EMPILER);
			po.produire(vCour);
			break;
		case 342:
			int k = presentIdent(1);
			if (tabSymb[k].categorie == CONSTANTE ) {
				po.produire( EMPILER );
				po.produire( tabSymb[k].info );
				tCour = tabSymb[k].type; 
			} else if (tabSymb[k].categorie == VARGLOBALE) { 
				po.produire ( CONTENUG );
				po.produire( tabSymb[k].info  );
				tCour = tabSymb[k].type; 
			} else {
			    UtilLex.messErr("Nul");
			}
			break;
		/*
		 * 
		 */
		case 351:
			tCour = ENT;
			vCour = UtilLex.valEnt;
			break;
		case 352:
			tCour = ENT;
			vCour = - UtilLex.valEnt;
			break;
		case 353:
			tCour = BOOL;
			vCour = VRAI;
			break;
		case 354:
			tCour = BOOL;
			vCour = FAUX;
			break;
		/*
		 * Affichage de la table des symboles
		 */
		case 400:
			afftabSymb();
			po.constObj();
			po.constGen();
			/* TODO : Verif pile vide */
			break;
		case 401:
			afftabSymb();
			System.out.println(tCour);
			System.out.println(vCour);
			break;
		default:
			System.out.println("Point de generation non prevu dans votre liste");
			break;

		}
	}
}