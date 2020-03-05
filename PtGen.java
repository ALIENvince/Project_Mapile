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
	 * Déclarations de variables utiles
	 */
	private static int cptVar;
	
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
	
	public static void initialisations() {
	
		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;
		
		// Compteur de variables/constante
		cptVar = 0;
		
		indexSymb=0;
		
		typeIdent=0;
		
		cptCond = 0;
		
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
		 * Déclaration de constante
		 */
			
		case 71:
			indexSymb = presentIdent(UtilLex.numIdCourant);
			//On verifie que la constante n'est pas dans la table des Symboles
			if (indexSymb < 1) {
			    placeIdent(UtilLex.numIdCourant, CONSTANTE, tCour, vCour);
			}
			else {
				UtilLex.messErr("La constante : " + UtilLex.chaineIdent(UtilLex.numIdCourant) +" est deja declaree" );
			}
			break;
		
		/*
		 * Déclaration de variable
		 */
		case 81:
			indexSymb = presentIdent(UtilLex.numIdCourant);
			//On verifie que la constante n'est pas dans la table des Symboles
			if(indexSymb < 1){
			    placeIdent(UtilLex.numIdCourant, VARGLOBALE, tCour, cptVar);
			    cptVar++;
			}
			else {
			    UtilLex.messErr("La variable : " + UtilLex.chaineIdent(UtilLex.numIdCourant) + " est deja declaree" );
			}
			break;
		case 82:
			//On reserve le nombre de variables vus lors du rajout dans tabSymb
			po.produire(RESERVER);
			po.produire(cptVar);
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

		/*
		 * Arret du programme
		 */
		case 131:
			po.produire(ARRET);
			break;
		/*
		 * Boucle if
		 */
		case 201:
			if(tCour == BOOL) {
				po.produire(BSIFAUX);
				po.produire(0);
				pileRep.empiler(po.getIpo());
			} else {
				UtilLex.messErr("Expression de si invalide");
			}
			break;
		case 202:
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(0);
			pileRep.empiler(po.getIpo());
			break;
		case 203:
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
			pileRep.empiler(po.getIpo());
			break;
		case 213:
			//maj bsifaux et depile la deuxieme valeur dans la pile
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(pileRep.depiler());
			pileRep.empiler(po.getIpo());
			break;
		case 214:
			po.produire(BINCOND);
			po.produire(po.getIpo()+1);
			break;
		case 215: //Remonter la pile de reprise
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
			
		case 221: //On empile la numéro de la ligne de la condition
			pileRep.empiler(po.getIpo()+1);
			break;
		case 222:
			if(tCour == BOOL ) { //Si la condition est de type booléen, on produit un BSIFAUX
				po.produire(BSIFAUX);
				po.produire(0);
				pileRep.empiler(po.getIpo());
			} else {
				UtilLex.messErr("Expression du ttq invalide");
			}
			break;
		case 223: //On modifie le bisfaux qui nous permet de sortir de la boucle si la condition est fausse, sinon on remonte vers la condition avec un bincond
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(pileRep.depiler());
			break;
			
		/*
		 * Lecture/Ecriture
		 */
		
		//Lecture	
		case 231:
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
					UtilLex.messErr("Le type de "+ row.code + " ne permet pas l'écriture");
				}
			} else {
				UtilLex.messErr(UtilLex.numIdCourant + " n'est pas dans la table des symboles");
			}
			break;
			
		//Ecriture	
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
			
		//Recuperation du type de l'ident, si il est present dans la table des symboles	
		case 251:
			indexSymb = presentIdent(UtilLex.numIdCourant);
			if(indexSymb != 0) {
				EltTabSymb row = tabSymb[indexSymb];
				int type = row.type;
				if(type == BOOL) {
				    typeIdent=BOOL;
				} else {
				    typeIdent=ENT;
				}
			} else {
				UtilLex.messErr(UtilLex.numIdCourant + " n'est pas dans la table des symboles");
			}
			break;
			
		//Verification de la concordance entre le type de l'ident et de l'expression que l'on lui affecte	
		case 252:
			if(typeIdent == tCour) { 
				EltTabSymb row = tabSymb[indexSymb];
				po.produire(AFFECTERG);
				po.produire(row.info);
			} else {
				UtilLex.messErr("Le type de l'ident et de l'expression sont incompatibles");
			}
			break;
			
		/*
		 * Expression OU
		 */
			
		// exp1 ou exp2
		case 281:
			po.produire(OU);
			tCour=BOOL;
			break;
			
		/*
		 * Expression ET
		 */
			
		// exp1 et exp2
		case 291:
			po.produire(ET);
			tCour=BOOL;
			break;
			
		/*
		 * Expression NON
		 */
			
		// non exp
		case 301:
			po.produire(NON);
			tCour=BOOL;
			break;
			
		/*
		 * Expressions EG/DIFF/SUP/SUPEG/INF/INFEG
		 */
			
		// '='	
		case 311:
			po.produire(EG);
			tCour=BOOL;
			break;
		// '<>'
		case 312:
			po.produire(DIFF);
			tCour=BOOL;
			break;
		// '>'
		case 313:
			po.produire(SUP);
			tCour=BOOL;
			break;
		// '>='
		case 314:
			po.produire(SUPEG);
			tCour=BOOL;
			break;
		// '<'
		case 315:
			po.produire(INF);
			tCour=BOOL;
			break;
		// '<='
		case 316:
			po.produire(INFEG);
			tCour=BOOL;
			break;
		/*
		 * Expressions + et -
		 */
		
		// '+'
		case 321:
			po.produire(ADD);
			tCour=ENT;
			break;
		// '-' 	
		case 322:
			po.produire(SOUS);
			tCour=ENT;
			break;
			
		/*
		 * Expressions * et div
		 */
			
		// '*'
		case 331:
			po.produire(MUL);
			tCour=ENT;
			break;
		// 'div'
		case 332:
			po.produire(DIV);
			tCour=ENT;
			break;
		/*
		 * Definition d'un type primaire
		 */
		
		//Valeur	
		case 341: 
			po.produire(EMPILER);
			po.produire(vCour);
			break;
		//Ident
		case 342:
			int k = presentIdent(1);
			if(k > 0) {
				if (tabSymb[k].categorie == CONSTANTE ) { //Si l'ident est une constante
					po.produire( EMPILER );
					po.produire( tabSymb[k].info );
					tCour = tabSymb[k].type; 
				} else if (tabSymb[k].categorie == VARGLOBALE) { //Si l'ident est un ident
					po.produire ( CONTENUG );
					po.produire( tabSymb[k].info  );
					tCour = tabSymb[k].type; 
				} else {
				    UtilLex.messErr("Type non reconnu");
				}
			} else {
				UtilLex.messErr("Ident non déclarée");
			}
			break;
			
		/*
		 * Définition des différentes valeurs
		 */
		
		//Entier positif
		case 351:
			tCour = ENT;
			vCour = UtilLex.valEnt;
			break;
		//Entier négatif
		case 352:
			tCour = ENT;
			vCour = - UtilLex.valEnt;
			break;
		//Booléen vrai
		case 353:
			tCour = BOOL;
			vCour = VRAI;
			break;
		//Booléan true
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
			/* Fct rajouté pour vérifier si la pile est vide */
			System.out.println(pileRep.isEmpty());
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