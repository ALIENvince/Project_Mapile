// Grammaire du langage PROJET
// CMPL L3info 
// Lohier, Faye, Jullion
// il convient d'y inserer les appels a {PtGen.pt(k);}
// relancer Antlr apres chaque modification et raffraichir le projet Eclipse le cas echeant

// attention l'analyse est poursuivie apres erreur si l'on supprime la clause rulecatch

grammar projet;

options {
  language=Java; k=1;
 }

@header {           
import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileInputStream;
} 


// partie syntaxique :  description de la grammaire //
// les non-terminaux doivent commencer par une minuscule


@members {

 
// variables globales et methodes utiles a placer ici
  
}
// la directive rulecatch permet d'interrompre l'analyse a la premiere erreur de syntaxe
@rulecatch {
catch (RecognitionException e) {reportError (e) ; throw e ; }}


unite  :   unitprog {PtGen.pt(400);} {PtGen.pt(11);} EOF
      |    unitmodule  EOF
  ;
  
unitprog
  : 'programme' ident ':'  
     declarations  
     corps { System.out.println("succes, arret de la compilation "); }
  ;
  
unitmodule
  : 'module' ident ':' 
     declarations   
  ;
  
declarations
  : partiedef? partieref? consts? vars? decprocs? 
  ;
  
partiedef
  : 'def' ident  (',' ident )* ptvg
  ;
  
partieref: 'ref'  specif (',' specif)* ptvg
  ;
  
specif  : ident  ( 'fixe' '(' type  ( ',' type  )* ')' )? 
                 ( 'mod'  '(' type  ( ',' type  )* ')' )? 
  ;
  
consts  : 'const' ( ident  '=' valeur {PtGen.pt(71);} ptvg  )+ 
  ;
  
vars  : 'var' ( type ident {PtGen.pt(81);}  ( ','  ident {PtGen.pt(81);} )* ptvg  )+ {PtGen.pt(82);}
  ;
  
type  : 'ent' {PtGen.pt(91);} 
  |     'bool' {PtGen.pt(92);}
  ;
  
decprocs: {PtGen.pt(101);} (decproc ptvg)+ {PtGen.pt(102);}
  ;
  
decproc :  'proc' ident {PtGen.pt(112);} parfixe? parmod? {PtGen.pt(113);} consts? vars? corps {PtGen.pt(118);}
  ;
  
ptvg  : ';'
  | 
  ;
  
corps : 'debut' instructions 'fin'
  ;
  
parfixe: 'fixe' '(' pf ( ';' pf)* ')'
  ;
  
pf  : type ident {PtGen.pt(151);} ( ',' ident {PtGen.pt(151);} )*  
  ;

parmod  : 'mod' '(' pm ( ';' pm)* ')'
  ;
  
pm  : type ident {PtGen.pt(171);} ( ',' ident {PtGen.pt(171);} )*
  ;
  
instructions
  : instruction ( ';' instruction)*
  ;
  
instruction
  : inssi
  | inscond
  | boucle
  | lecture
  | ecriture
  | affouappel
  |
  ;
  
inssi : 'si' expression {PtGen.pt(201);} 'alors' instructions ('sinon' {PtGen.pt(202);} instructions)? 'fsi'{PtGen.pt(203);}
  ;
  
inscond : 'cond' {PtGen.pt(211);} expression {PtGen.pt(212);} ':' instructions 
          (',' {PtGen.pt(213);} expression {PtGen.pt(212);} ':' instructions )* 
          ('aut' {PtGen.pt(213);} instructions | {PtGen.pt(214);} ) 
          'fcond' {PtGen.pt(215);}
  ;
  
boucle  : 'ttq' {PtGen.pt(221);} expression {PtGen.pt(222);} 'faire' instructions 'fait' {PtGen.pt(223);}
  ;
  
lecture: 'lire' '(' ident {PtGen.pt(231);} ( ',' ident {PtGen.pt(231);}  )* ')' 
  ;
  
ecriture: 'ecrire' '(' expression {PtGen.pt(241);} ( ',' expression {PtGen.pt(241);} )* ')'
   ;
  
affouappel
  : ident {PtGen.pt(251);} ( ':=' expression {PtGen.pt(252);}
            | {PtGen.pt(253);} (effixes {PtGen.pt(254);} (effmods)? {PtGen.pt(255);} )? {PtGen.pt(256);}
           )
  ;
  
effixes : '(' (expression  {PtGen.pt(261);} (',' expression {PtGen.pt(261);} )*)? ')'
  ;
  
effmods :'(' (ident {PtGen.pt(271);} (',' ident {PtGen.pt(271);} )*)? ')'
  ; 
  
expression: (exp1) ('ou' {PtGen.pt(402);}  exp1 {PtGen.pt(402);} {PtGen.pt(281);} )*
  ;
  
exp1  : exp2 ('et' {PtGen.pt(402);} exp2 {PtGen.pt(402);} {PtGen.pt(291);}  )*
  ;
  
exp2  : 'non' exp2 {PtGen.pt(402);} {PtGen.pt(301);}
  | exp3
  ;
  
exp3  : exp4 
  ( '='  {PtGen.pt(401);} exp4 {PtGen.pt(401);} {PtGen.pt(311);}
  | '<>' {PtGen.pt(401);} exp4 {PtGen.pt(401);} {PtGen.pt(312);}
  | '>'  {PtGen.pt(401);} exp4 {PtGen.pt(401);} {PtGen.pt(313);}
  | '>=' {PtGen.pt(401);} exp4 {PtGen.pt(401);} {PtGen.pt(314);}
  | '<'  {PtGen.pt(401);} exp4 {PtGen.pt(401);} {PtGen.pt(315);}
  | '<=' {PtGen.pt(401);} exp4 {PtGen.pt(401);} {PtGen.pt(316);}
  ) ?
  ;
  
exp4  : exp5 
        ('+' {PtGen.pt(401);} exp5 {PtGen.pt(401);} {PtGen.pt(321);}
        |'-' {PtGen.pt(401);} exp5 {PtGen.pt(401);} {PtGen.pt(322);}
        )*
  ;
  
exp5  : primaire 
        (    '*'  {PtGen.pt(401);} primaire {PtGen.pt(401);} {PtGen.pt(331);}
          | 'div' {PtGen.pt(401);} primaire {PtGen.pt(401);} {PtGen.pt(332);}
        )*
  ;
  
primaire: valeur {PtGen.pt(341);}
  | ident  {PtGen.pt(342);}
  | '(' expression ')'
  ;
  
valeur  : nbentier {PtGen.pt(351);}
  | '+' nbentier {PtGen.pt(351);}
  | '-' nbentier {PtGen.pt(352);}
  | 'vrai' {PtGen.pt(353);}
  | 'faux' {PtGen.pt(354);}
  ;

// partie lexicale  : cette partie ne doit pas etre modifiee  //
// les unites lexicales de ANTLR doivent commencer par une majuscule
// Attention : ANTLR n'autorise pas certains traitements sur les unites lexicales, 
// il est alors ncessaire de passer par un non-terminal intermediaire 
// exemple : pour l'unit lexicale INT, le non-terminal nbentier a du etre introduit
 
      
nbentier  :   INT { UtilLex.valEnt = Integer.parseInt($INT.text);}; // mise a jour de valEnt

ident : ID  { UtilLex.traiterId($ID.text); } ; // mise a jour de numIdCourant
     // tous les identificateurs seront places dans la table des identificateurs, y compris le nom du programme ou module
     // (NB: la table des symboles n'est pas geree au niveau lexical mais au niveau du compilateur)
        
  
ID  :   ('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'0'..'9'|'_')* ; 
     
// zone purement lexicale //

INT :   '0'..'9'+ ;
WS  :   (' '|'\t' |'\r')+ {skip();} ; // definition des "blocs d'espaces"
RC  :   ('\n') {UtilLex.incrementeLigne(); skip() ;} ; // definition d'un unique "passage a la ligne" et comptage des numeros de lignes

COMMENT
  :  '\{' (.)* '\}' {skip();}   // toute suite de caracteres entouree d'accolades est un commentaire
  |  '#' ~( '\r' | '\n' )* {skip();}  // tout ce qui suit un caractere diese sur une ligne est un commentaire
  ;

// commentaires sur plusieurs lignes
ML_COMMENT    :   '/*' (options {greedy=false;} : .)* '*/' {$channel=HIDDEN;}
    ;	   



	   
