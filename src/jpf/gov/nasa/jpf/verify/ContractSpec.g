grammar ContractSpec;

options {
	language=Java;
	backtrack=true;
}

@header {
	package gov.nasa.jpf.test;	
}

@lexer::header {
	package gov.nasa.jpf.test;
}

@members {
    ContractContext ctx;
    
    public ContractSpecParser (TokenStream input, ContractContext ctx) {
      this(input);
      
      this.ctx = ctx;
    }
}


/***************************** rules **************************************************************/

/* just for debugging purposes */
contractSpec 
    :	
    contract                         { System.out.println($contract.c); }
	;

contract returns [Contract c]
    :
    c1=contractAnd                   { c=$c1.c; }
    ('||' c2=contractAnd             { c= new ContractOr(c,$c2.c); } 
    )*
    ;

contractAnd returns [Contract c]
    :
    c1=contractAtom                  { c=$c1.c; }
    ('&&' c2=contractAtom            { c= new ContractAnd(c,$c2.c); }
    )*
	;

contractAtom returns [Contract c]
    : 
	simpleContract                   { c= $simpleContract.c; }
	| '(' contract ')'               { c= $contract.c; }
	;
	
simpleContract returns [Contract c]
	:
	e1=expr
	(
	'==' e2=expr                     { c = new Contract.EQ($e1.o, $e2.o); } 
	| '!=' e3=expr                   { c = new Contract.NE($e1.o, $e3.o); }
	| '>' e4=expr                    { c = new Contract.GT($e1.o, $e4.o); }
	| '>=' e5=expr                   { c = new Contract.GE($e1.o, $e5.o); }
	| '<' e6=expr                    { c = new Contract.LT($e1.o, $e6.o); }
	| '<=' e7=expr                   { c = new Contract.LE($e1.o, $e7.o); }
	| 'within' e8=expr 
	    ( ',' e9=expr                { c = new Contract.Within( $e1.o, $e8.o, $e9.o); }
	    | '+-' e10=expr              { c = new Contract.WithinCenter( $e1.o, $e8.o, $e10.o); }
	    )
	| 'isEmpty'                      { c = new Contract.IsEmpty( $e1.o); }
    | 'notEmpty'                     { c = new Contract.NotEmpty( $e1.o); }
    | 'instanceof' ID                { c = new Contract.InstanceOf( $e1.o, $ID.text); }
    | 'matches' STRING               { c = new Contract.Matches( $e1.o, $STRING.text); }
    | genericContract[$e1.o]         { c = $genericContract.c; }
	)
	;
	
genericContract [Operand o] returns [Contract c]
    @init {
    	ArrayList<Operand> args = null;
    	String id = null;
    }
    @after {
        c = new Contract.Satisfies(ctx, id, o, args);
    }
	:
	 'satisfies' ID                  { id=$ID.text; }
	  ('('                           
	      (o1=expr                   { args = new ArrayList<Operand>(); args.add($o1.o); }
	       (',' o2=expr              { args.add($o2.o); }
	       )*
	      )?
	   ')'                           
	  )?
	;


expr returns [Operand o]
    :
    o1=mult                          { o = $o1.o; }
    ( '+' o2=mult                    { o = new Expr.Plus(o, $o2.o); }
    | '-' o3=mult                    { o = new Expr.Minus(o, $o3.o); }
    )*
    ;
    
mult returns [Operand o]
    :	 
	o1=log                           { o = $o1.o; }
	( '*' o2=log                     { o = new Expr.Mult(o,$o2.o); }
    | '/' o3=log                     { o = new Expr.Div(o, $o3.o); }
    )*
    ;

log	 returns [Operand o]
    :	
    o1=exp                           { o = $o1.o; }
	| 'log10' '(' o3=exp ')'         { o = new Expr.Log10($o3.o); }
	| 'log' '(' o2=exp ')'           { o = new Expr.Log($o2.o); }
	;
	
exp  returns [Operand o]
    :	
	o1=fun                           { o = $o1.o; }
	('^' o2=fun                      { o = new Expr.Pow(o, $o2.o); }
	)?
	;
	
fun  returns [Operand o]
    @init {
    	ArrayList<Operand> args = new ArrayList<Operand>();
    	String id = "?";
    }
    :	
	atom                             { o = $atom.o; } 
	| ( ID '('                       { id=$ID.text; }
	    (o1=expr                     { args = new ArrayList<Operand>(); args.add($o1.o); }
	     (',' o2=expr                { args.add($o2.o); }
	     )*
	    )?
	    ')'                          { o = new Expr.Func(id,args); }
	  )
	;

atom returns [Operand o]:
    'null'                           { o = Operand.NULL; }
    | 'return'                       { o = new Expr.Result(); }
    | 'EPS'                          { o = Operand.EPS; }
	| numOrVar                       { o = $numOrVar.o; }
	| STRING                         { o = new Operand.Const($STRING.text); }
	| 'old' '(' e1=expr ')'          { o = new Expr.Old($e1.o); }
	| '(' e2=expr ')'                { o = $e2.o; }
	;

numOrVar returns [Operand o]:
    '-'?
	(i2=ID            	             { o = new Operand.VarRef($i2.text); }
	| INT                            { o = new Operand.Const(new Integer($INT.text)); }
	| REAL                           { o = new Operand.Const(new Double($REAL.text)); }
	)
	;

	
/***************************** lexer **************************************************************/

ID :  ('a'..'z'|'A'..'Z'|'$'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'$'|'_'|'.')*;

fragment
SIGN: ('-'|'+');

fragment
NUM : ('0'..'9')+;

INT :  NUM;

REAL: NUM? '.' NUM ('e' SIGN? NUM)?;


fragment
ESCAPE
  :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
  ;

STRING
  @after {  // remove the quotes
    String s = getText();
    setText( s.substring(1, s.length()-1));
  }
  :  '\'' ( ESCAPE | ~('\\'|'\'') )* '\''
  ;

STRING_PATTERN
  @after {  // remove the quotes
    String s = getText();
    setText( s.substring(2, s.length()-1));
  }
  : '#\'' ( ESCAPE | ~('\\'|'\'') )* '\''
  ;
      
   
// add regex strings
  
WS  : (' '|'\t')+  {skip();};
