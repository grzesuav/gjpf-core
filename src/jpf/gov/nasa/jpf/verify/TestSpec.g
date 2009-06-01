grammar TestSpec;

options {
	language=Java;
	backtrack=true;
}

@header {
	package gov.nasa.jpf.test;
	
    import gov.nasa.jpf.util.StringExpander;
    import java.io.PrintWriter;
    import java.util.List;
    import java.util.ArrayList;
}

@lexer::header {
	package gov.nasa.jpf.test;
}

@members {
    // to report errors that are not grammar related
    PrintWriter err = new PrintWriter(System.err, true);

	StringExpander exp = new StringExpander();
	TestContext  tctx;

    List<String> expandStrings (String sp){
    	return exp.expand(sp);
    }
        
    List<Integer> expandInts (String sp){
    	List<String> sl = exp.expand(sp);
        ArrayList<Integer> list = new ArrayList<Integer>(sl.size());
        for (String s : sl){
        	list.add( new Integer(s));
        }
        return list;
    }
        
    List<Double> expandReals (String sp){
        List<String> sl = exp.expand(sp);
        ArrayList<Double> list = new ArrayList<Double>(sl.size());
        for (String s : sl){
            list.add( new Double(s));
        }
        return list;
    }
    
    List<Object> createLists (ArgList list) {
    	List<Object> ol = new ArrayList<Object>();
 		for (Object[] a : list.getArgCombinations()){
 			ol.add(a);
		}    	
		return ol;
    }
    
    List<Object> createObjects (String cls, ArgList args) {
    	List<Object> ol = new ArrayList<Object>();
    	if (args != null) {
    		for (Object[] a : args.getArgCombinations()){
    			addObject(cls,a,ol);
    		}
    	} else {
    		addObject(cls, null, ol);
    	}
    	return ol;
    }
    
    void addObject (String cls, Object[] args, List<Object> list){
		Object o = tctx.create(cls,args);
    	if (o != null){
			list.add(o);
    	}
    }

	Goal createGoal (String id, ArgList args) {
		Goal g = tctx.createGoal(id, args);
		return g;
	}

	public TestSpecParser(TokenStream input, PrintWriter err, TestContext tctx) {
		this(input);
		
		this.err = err;
		this.tctx = tctx;
	}
	
}

/***************************** rules **************************************************************/

testspec returns [TestSpec spec] 
	@init {
		spec = new TestSpec();
	}
	@after {
		//$spec.printOn(new PrintWriter(System.out));
	}
	: (e1= env       		      { spec.addTargetArgs($e1.a); }
	    ('|' e2= env              { spec.addTargetArgs($e2.a); }
	    )*
	    '.'
	  )?
	  a1= arglist                 { spec.addCallArgs($a1.a); }
	  ('|' a2= arglist            { spec.addCallArgs($a2.a); }
	  )*
	  (g1= goal                   { spec.addGoal($g1.g); }
	    (',' g2= goal             { spec.addGoal($g2.g); }
	    )*
	  )?
	;
	
env returns [ArgList a] 
	: 'this' arglist              { a = $arglist.a; } 
	;
	  
arglist returns [ArgList a]
	@init {
		a = new ArgList();
	} 
	: '('
	     (a1= arg                 { a.add($a1.s); }
	       ( ',' a2= arg          { a.add($a2.s); }
	       )*
	     )? 
	  ')' 
	;
	
arg returns [ValSet s]
	@init {
		s = new ValSet();
	}
	: int_arg[s]
 	| real_arg[s]
 	| string_arg[s]
 	| boolean_arg[s]
 	| object_arg[s]
 	| field_arg[s]
 	| list_arg[s]
 	;


list_arg [ValSet s]
    : l1= list                    { s.addAll( createLists( $l1.a)); }
      ('|' l2= list               { s.addAll( createLists( $l2.a)); }
      )*
    ;

int_arg [ValSet s]
	: (a1= INT                    { s.add( new Integer($a1.text)); }
	  | p1= INT_PATTERN           { s.addAll( expandInts($p1.text)); }
	  )
	  ('|' (a2= INT               { s.add( new Integer($a2.text)); }
	       | p2= INT_PATTERN      { s.addAll( expandInts($p2.text)); }
	       )
	  )*
	;
	
real_arg [ValSet s]
	: (a1= REAL                   { s.add( new Double($a1.text)); }
	  | p1= REAL_PATTERN          { s.addAll( expandReals($p1.text)); }
	  )
	  ('|' (a2= REAL              { s.add( new Double($a2.text)); }
	       | p2= REAL_PATTERN     { s.addAll( expandReals($p2.text)); }
	       )
	  )*
	;
	
string_arg [ValSet s]
	: (a1= STRING                 { s.add( $a1.text); }
	  | p1= STRING_PATTERN        { s.addAll( expandStrings( $p1.text)); }
	  )
	  ( '|' (a2= STRING           { s.add( $a2.text); }
	        | p2= STRING_PATTERN  { s.addAll( expandStrings( $p2.text)); }
	        )
	  )*
	;

boolean_arg [ValSet s]
	: 'true'                      { s.add(Boolean.TRUE); }
	| 'false'                     { s.add(Boolean.FALSE); }
	;

field_arg [ValSet s]
	: i1= ID                      { s.add( new FieldReference($i1.text)); }
	  ( '|' i2= ID                { s.add( new FieldReference($i2.text)); }
	  )*
	; 

object_arg [ValSet s] 
	: a1= object                  { s.addAll( $a1.s.getValues()); }
	  ( '|' a2= object            { s.addAll( $a2.s.getValues()); }
	  )*
	;

object returns [ValSet s]
	@init {
		s = new ValSet();
	}
    : 'new' ID arglist            { s.addAll( createObjects($ID.text, $arglist.a)); }
    ;

list returns [ArgList a]
    @init {
        a = new ArgList();
    }
    : '{'
        (a1= arg                  { a.add($a1.s); }
          (',' a2= arg            { a.add($a2.s); }
          )*
        )?
      '}'
    ;

num returns [Object o]
	: INT                         { o = new Integer($INT.text); }
	| REAL                        { o = new Double($REAL.text); }
	;

goal returns [Goal g]
	: compareGoal                 { g = $compareGoal.g; }
	| matchGoal                   { g = $matchGoal.g; }         
	| withinGoal                  { g = $withinGoal.g; }
	| throwsGoal                  { g = $throwsGoal.g; }
	| noThrowsGoal                { g = $noThrowsGoal.g; }
	| satisfiesGoal               { g = $satisfiesGoal.g; }
	;

// <2do> maybe we should switch the Lx/Gx comparisons to 'num' instead of 'arg'
// since sets don't make much sense here
compareGoal returns [Goal g] 
	: '==' a1=arg                 { g = new CompareGoal(CompareGoal.Operator.EQ, $a1.s); }
	| '!=' a2=arg                 { g = new CompareGoal(CompareGoal.Operator.NE, $a2.s); }
	| '<'  a3=arg                 { g = new CompareGoal(CompareGoal.Operator.LT, $a3.s); }
	| '<=' a4=arg                 { g = new CompareGoal(CompareGoal.Operator.LE, $a4.s); }
	| '>'  a5=arg                 { g = new CompareGoal(CompareGoal.Operator.GT, $a5.s); }
	| '>=' a6=arg                 { g = new CompareGoal(CompareGoal.Operator.GE, $a6.s); }
	;

matchGoal returns [Goal g] 
	: 'matches' STRING            { g = new RegexGoal( $STRING.text); } 
	;

withinGoal returns [Goal g] 
    @init {
    	boolean isDelta=false;
    }
	: 'within'
	  a1= intervalArg 
	  (','
	  |'+-'                       { isDelta = true; }
	  )
	  a2= intervalArg             { g = new WithinGoal( $a1.o, $a2.o, isDelta); }
	;
	
intervalArg returns [Object o]
	: INT                         { o = new Integer($INT.text); }
	| REAL                        { o = new Double($REAL.text); }
	| ID                          { o = new FieldReference( $ID.text); }
	;
	
	
throwsGoal returns [Goal g] 
	: 'throws' 
	   ( ID                       { g = new ThrowsGoal( $ID.text); }
	   | ID_PATTERN               { g = new ThrowsGoal( $ID_PATTERN.text); }
	   )
	;
	
noThrowsGoal returns [Goal g] 
	: 'noThrows'                  { g = new NoThrowsGoal(); }
	;

// this is the generic verification goal
satisfiesGoal returns [Goal g]
	@init {
		ArgList a = null;
	} 
	: 'satisfies' ID
	  (arglist                     { a = $arglist.a; }
	  )?                           { g = createGoal($ID.text, a); }
	;


/***************************** lexer **************************************************************/

ID :  ('a'..'z'|'A'..'Z'|'$'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'$'|'_'|'.')*;

ID_PATTERN
  : ('a'..'z'|'A'..'Z'|'$'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'$'|'_'|'.'|'*')*;

fragment
SIGN: ('-'|'+');

fragment
NUM : ('0'..'9')+;

fragment
NUM_PATTERN
  : ('0'..'9' | '[' | ']')+;   // <2do> - collides with arrays

INT : SIGN? NUM;

INT_PATTERN
  : SIGN? NUM_PATTERN;

REAL: (SIGN? NUM)? '.' NUM ('e' SIGN? NUM)?;

REAL_PATTERN
  : (SIGN? NUM_PATTERN)? '.' NUM_PATTERN ('e' SIGN? NUM_PATTERN)?;

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
