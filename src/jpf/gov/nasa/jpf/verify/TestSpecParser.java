// $ANTLR 3.1.2 /Users/pcmehlitz/projects/grammars/TestSpec.g 2009-05-05 15:46:40

	package gov.nasa.jpf.verify;
	
    import gov.nasa.jpf.util.StringExpander;
    import java.io.PrintWriter;
    import java.util.List;
    import java.util.ArrayList;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
public class TestSpecParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "INT", "INT_PATTERN", "REAL", "REAL_PATTERN", "STRING", "STRING_PATTERN", "ID", "ID_PATTERN", "SIGN", "NUM", "NUM_PATTERN", "ESCAPE", "WS", "'|'", "'.'", "','", "'this'", "'('", "')'", "'true'", "'false'", "'new'", "'{'", "'}'", "'=='", "'!='", "'<'", "'<='", "'>'", "'>='", "'matches'", "'within'", "'+-'", "'throws'", "'noThrows'", "'satisfies'"
    };
    public static final int SIGN=12;
    public static final int T__29=29;
    public static final int T__28=28;
    public static final int T__27=27;
    public static final int T__26=26;
    public static final int T__25=25;
    public static final int T__24=24;
    public static final int T__23=23;
    public static final int T__22=22;
    public static final int T__21=21;
    public static final int T__20=20;
    public static final int ID_PATTERN=11;
    public static final int INT=4;
    public static final int ID=10;
    public static final int EOF=-1;
    public static final int NUM=13;
    public static final int STRING_PATTERN=9;
    public static final int REAL=6;
    public static final int T__19=19;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int WS=16;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int ESCAPE=15;
    public static final int T__35=35;
    public static final int T__18=18;
    public static final int T__36=36;
    public static final int T__17=17;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int NUM_PATTERN=14;
    public static final int INT_PATTERN=5;
    public static final int REAL_PATTERN=7;
    public static final int STRING=8;

    // delegates
    // delegators


        public TestSpecParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public TestSpecParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return TestSpecParser.tokenNames; }
    public String getGrammarFileName() { return "/Users/pcmehlitz/projects/grammars/TestSpec.g"; }


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
    	



    // $ANTLR start "testspec"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:91:1: testspec returns [TestSpec spec] : (e1= env ( '|' e2= env )* '.' )? a1= arglist ( '|' a2= arglist )* (g1= goal ( ',' g2= goal )* )? ;
    public final TestSpec testspec() throws RecognitionException {
        TestSpec spec = null;

        ArgList e1 = null;

        ArgList e2 = null;

        ArgList a1 = null;

        ArgList a2 = null;

        Goal g1 = null;

        Goal g2 = null;



        		spec = new TestSpec();
        	
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:100:2: ( (e1= env ( '|' e2= env )* '.' )? a1= arglist ( '|' a2= arglist )* (g1= goal ( ',' g2= goal )* )? )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:100:4: (e1= env ( '|' e2= env )* '.' )? a1= arglist ( '|' a2= arglist )* (g1= goal ( ',' g2= goal )* )?
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:100:4: (e1= env ( '|' e2= env )* '.' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==20) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:100:5: e1= env ( '|' e2= env )* '.'
                    {
                    pushFollow(FOLLOW_env_in_testspec73);
                    e1=env();

                    state._fsp--;
                    if (state.failed) return spec;
                    if ( state.backtracking==0 ) {
                       spec.addTargetArgs(e1); 
                    }
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:101:6: ( '|' e2= env )*
                    loop1:
                    do {
                        int alt1=2;
                        int LA1_0 = input.LA(1);

                        if ( (LA1_0==17) ) {
                            alt1=1;
                        }


                        switch (alt1) {
                    	case 1 :
                    	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:101:7: '|' e2= env
                    	    {
                    	    match(input,17,FOLLOW_17_in_testspec97); if (state.failed) return spec;
                    	    pushFollow(FOLLOW_env_in_testspec102);
                    	    e2=env();

                    	    state._fsp--;
                    	    if (state.failed) return spec;
                    	    if ( state.backtracking==0 ) {
                    	       spec.addTargetArgs(e2); 
                    	    }

                    	    }
                    	    break;

                    	default :
                    	    break loop1;
                        }
                    } while (true);

                    match(input,18,FOLLOW_18_in_testspec132); if (state.failed) return spec;

                    }
                    break;

            }

            pushFollow(FOLLOW_arglist_in_testspec146);
            a1=arglist();

            state._fsp--;
            if (state.failed) return spec;
            if ( state.backtracking==0 ) {
               spec.addCallArgs(a1); 
            }
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:106:4: ( '|' a2= arglist )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==17) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:106:5: '|' a2= arglist
            	    {
            	    match(input,17,FOLLOW_17_in_testspec170); if (state.failed) return spec;
            	    pushFollow(FOLLOW_arglist_in_testspec175);
            	    a2=arglist();

            	    state._fsp--;
            	    if (state.failed) return spec;
            	    if ( state.backtracking==0 ) {
            	       spec.addCallArgs(a2); 
            	    }

            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);

            // /Users/pcmehlitz/projects/grammars/TestSpec.g:108:4: (g1= goal ( ',' g2= goal )* )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( ((LA5_0>=28 && LA5_0<=35)||(LA5_0>=37 && LA5_0<=39)) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:108:5: g1= goal ( ',' g2= goal )*
                    {
                    pushFollow(FOLLOW_goal_in_testspec203);
                    g1=goal();

                    state._fsp--;
                    if (state.failed) return spec;
                    if ( state.backtracking==0 ) {
                       spec.addGoal(g1); 
                    }
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:109:6: ( ',' g2= goal )*
                    loop4:
                    do {
                        int alt4=2;
                        int LA4_0 = input.LA(1);

                        if ( (LA4_0==19) ) {
                            alt4=1;
                        }


                        switch (alt4) {
                    	case 1 :
                    	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:109:7: ',' g2= goal
                    	    {
                    	    match(input,19,FOLLOW_19_in_testspec231); if (state.failed) return spec;
                    	    pushFollow(FOLLOW_goal_in_testspec236);
                    	    g2=goal();

                    	    state._fsp--;
                    	    if (state.failed) return spec;
                    	    if ( state.backtracking==0 ) {
                    	       spec.addGoal(g2); 
                    	    }

                    	    }
                    	    break;

                    	default :
                    	    break loop4;
                        }
                    } while (true);


                    }
                    break;

            }


            }

            if ( state.backtracking==0 ) {

              		//spec.printOn(new PrintWriter(System.out));
              	
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return spec;
    }
    // $ANTLR end "testspec"


    // $ANTLR start "env"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:114:1: env returns [ArgList a] : 'this' arglist ;
    public final ArgList env() throws RecognitionException {
        ArgList a = null;

        ArgList arglist1 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:115:2: ( 'this' arglist )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:115:4: 'this' arglist
            {
            match(input,20,FOLLOW_20_in_env281); if (state.failed) return a;
            pushFollow(FOLLOW_arglist_in_env283);
            arglist1=arglist();

            state._fsp--;
            if (state.failed) return a;
            if ( state.backtracking==0 ) {
               a = arglist1; 
            }

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return a;
    }
    // $ANTLR end "env"


    // $ANTLR start "arglist"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:118:1: arglist returns [ArgList a] : '(' (a1= arg ( ',' a2= arg )* )? ')' ;
    public final ArgList arglist() throws RecognitionException {
        ArgList a = null;

        ValSet a1 = null;

        ValSet a2 = null;



        		a = new ArgList();
        	
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:122:2: ( '(' (a1= arg ( ',' a2= arg )* )? ')' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:122:4: '(' (a1= arg ( ',' a2= arg )* )? ')'
            {
            match(input,21,FOLLOW_21_in_arglist324); if (state.failed) return a;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:123:7: (a1= arg ( ',' a2= arg )* )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( ((LA7_0>=INT && LA7_0<=ID)||(LA7_0>=23 && LA7_0<=26)) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:123:8: a1= arg ( ',' a2= arg )*
                    {
                    pushFollow(FOLLOW_arg_in_arglist336);
                    a1=arg();

                    state._fsp--;
                    if (state.failed) return a;
                    if ( state.backtracking==0 ) {
                       a.add(a1); 
                    }
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:124:9: ( ',' a2= arg )*
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( (LA6_0==19) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:124:11: ',' a2= arg
                    	    {
                    	    match(input,19,FOLLOW_19_in_arglist366); if (state.failed) return a;
                    	    pushFollow(FOLLOW_arg_in_arglist371);
                    	    a2=arg();

                    	    state._fsp--;
                    	    if (state.failed) return a;
                    	    if ( state.backtracking==0 ) {
                    	       a.add(a2); 
                    	    }

                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);


                    }
                    break;

            }

            match(input,22,FOLLOW_22_in_arglist408); if (state.failed) return a;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return a;
    }
    // $ANTLR end "arglist"


    // $ANTLR start "arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:130:1: arg returns [ValSet s] : ( int_arg[s] | real_arg[s] | string_arg[s] | boolean_arg[s] | object_arg[s] | field_arg[s] | list_arg[s] );
    public final ValSet arg() throws RecognitionException {
        ValSet s = null;


        		s = new ValSet();
        	
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:134:2: ( int_arg[s] | real_arg[s] | string_arg[s] | boolean_arg[s] | object_arg[s] | field_arg[s] | list_arg[s] )
            int alt8=7;
            switch ( input.LA(1) ) {
            case INT:
            case INT_PATTERN:
                {
                alt8=1;
                }
                break;
            case REAL:
            case REAL_PATTERN:
                {
                alt8=2;
                }
                break;
            case STRING:
            case STRING_PATTERN:
                {
                alt8=3;
                }
                break;
            case 23:
            case 24:
                {
                alt8=4;
                }
                break;
            case 25:
                {
                alt8=5;
                }
                break;
            case ID:
                {
                alt8=6;
                }
                break;
            case 26:
                {
                alt8=7;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return s;}
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }

            switch (alt8) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:134:4: int_arg[s]
                    {
                    pushFollow(FOLLOW_int_arg_in_arg431);
                    int_arg(s);

                    state._fsp--;
                    if (state.failed) return s;

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:135:5: real_arg[s]
                    {
                    pushFollow(FOLLOW_real_arg_in_arg438);
                    real_arg(s);

                    state._fsp--;
                    if (state.failed) return s;

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:136:5: string_arg[s]
                    {
                    pushFollow(FOLLOW_string_arg_in_arg445);
                    string_arg(s);

                    state._fsp--;
                    if (state.failed) return s;

                    }
                    break;
                case 4 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:137:5: boolean_arg[s]
                    {
                    pushFollow(FOLLOW_boolean_arg_in_arg452);
                    boolean_arg(s);

                    state._fsp--;
                    if (state.failed) return s;

                    }
                    break;
                case 5 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:138:5: object_arg[s]
                    {
                    pushFollow(FOLLOW_object_arg_in_arg459);
                    object_arg(s);

                    state._fsp--;
                    if (state.failed) return s;

                    }
                    break;
                case 6 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:139:5: field_arg[s]
                    {
                    pushFollow(FOLLOW_field_arg_in_arg466);
                    field_arg(s);

                    state._fsp--;
                    if (state.failed) return s;

                    }
                    break;
                case 7 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:140:5: list_arg[s]
                    {
                    pushFollow(FOLLOW_list_arg_in_arg473);
                    list_arg(s);

                    state._fsp--;
                    if (state.failed) return s;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return s;
    }
    // $ANTLR end "arg"


    // $ANTLR start "list_arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:144:1: list_arg[ValSet s] : l1= list ( '|' l2= list )* ;
    public final void list_arg(ValSet s) throws RecognitionException {
        ArgList l1 = null;

        ArgList l2 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:145:5: (l1= list ( '|' l2= list )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:145:7: l1= list ( '|' l2= list )*
            {
            pushFollow(FOLLOW_list_in_list_arg495);
            l1=list();

            state._fsp--;
            if (state.failed) return ;
            if ( state.backtracking==0 ) {
               s.addAll( createLists( l1)); 
            }
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:146:7: ( '|' l2= list )*
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( (LA9_0==17) ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:146:8: '|' l2= list
            	    {
            	    match(input,17,FOLLOW_17_in_list_arg525); if (state.failed) return ;
            	    pushFollow(FOLLOW_list_in_list_arg530);
            	    l2=list();

            	    state._fsp--;
            	    if (state.failed) return ;
            	    if ( state.backtracking==0 ) {
            	       s.addAll( createLists( l2)); 
            	    }

            	    }
            	    break;

            	default :
            	    break loop9;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "list_arg"


    // $ANTLR start "int_arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:150:1: int_arg[ValSet s] : (a1= INT | p1= INT_PATTERN ) ( '|' (a2= INT | p2= INT_PATTERN ) )* ;
    public final void int_arg(ValSet s) throws RecognitionException {
        Token a1=null;
        Token p1=null;
        Token a2=null;
        Token p2=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:151:2: ( (a1= INT | p1= INT_PATTERN ) ( '|' (a2= INT | p2= INT_PATTERN ) )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:151:4: (a1= INT | p1= INT_PATTERN ) ( '|' (a2= INT | p2= INT_PATTERN ) )*
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:151:4: (a1= INT | p1= INT_PATTERN )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==INT) ) {
                alt10=1;
            }
            else if ( (LA10_0==INT_PATTERN) ) {
                alt10=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:151:5: a1= INT
                    {
                    a1=(Token)match(input,INT,FOLLOW_INT_in_int_arg575); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.add( new Integer((a1!=null?a1.getText():null))); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:152:6: p1= INT_PATTERN
                    {
                    p1=(Token)match(input,INT_PATTERN,FOLLOW_INT_PATTERN_in_int_arg606); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.addAll( expandInts((p1!=null?p1.getText():null))); 
                    }

                    }
                    break;

            }

            // /Users/pcmehlitz/projects/grammars/TestSpec.g:154:4: ( '|' (a2= INT | p2= INT_PATTERN ) )*
            loop12:
            do {
                int alt12=2;
                int LA12_0 = input.LA(1);

                if ( (LA12_0==17) ) {
                    alt12=1;
                }


                switch (alt12) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:154:5: '|' (a2= INT | p2= INT_PATTERN )
            	    {
            	    match(input,17,FOLLOW_17_in_int_arg629); if (state.failed) return ;
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:154:9: (a2= INT | p2= INT_PATTERN )
            	    int alt11=2;
            	    int LA11_0 = input.LA(1);

            	    if ( (LA11_0==INT) ) {
            	        alt11=1;
            	    }
            	    else if ( (LA11_0==INT_PATTERN) ) {
            	        alt11=2;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return ;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("", 11, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt11) {
            	        case 1 :
            	            // /Users/pcmehlitz/projects/grammars/TestSpec.g:154:10: a2= INT
            	            {
            	            a2=(Token)match(input,INT,FOLLOW_INT_in_int_arg635); if (state.failed) return ;
            	            if ( state.backtracking==0 ) {
            	               s.add( new Integer((a2!=null?a2.getText():null))); 
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/pcmehlitz/projects/grammars/TestSpec.g:155:11: p2= INT_PATTERN
            	            {
            	            p2=(Token)match(input,INT_PATTERN,FOLLOW_INT_PATTERN_in_int_arg666); if (state.failed) return ;
            	            if ( state.backtracking==0 ) {
            	               s.addAll( expandInts((p2!=null?p2.getText():null))); 
            	            }

            	            }
            	            break;

            	    }


            	    }
            	    break;

            	default :
            	    break loop12;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "int_arg"


    // $ANTLR start "real_arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:160:1: real_arg[ValSet s] : (a1= REAL | p1= REAL_PATTERN ) ( '|' (a2= REAL | p2= REAL_PATTERN ) )* ;
    public final void real_arg(ValSet s) throws RecognitionException {
        Token a1=null;
        Token p1=null;
        Token a2=null;
        Token p2=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:161:2: ( (a1= REAL | p1= REAL_PATTERN ) ( '|' (a2= REAL | p2= REAL_PATTERN ) )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:161:4: (a1= REAL | p1= REAL_PATTERN ) ( '|' (a2= REAL | p2= REAL_PATTERN ) )*
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:161:4: (a1= REAL | p1= REAL_PATTERN )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==REAL) ) {
                alt13=1;
            }
            else if ( (LA13_0==REAL_PATTERN) ) {
                alt13=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;
            }
            switch (alt13) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:161:5: a1= REAL
                    {
                    a1=(Token)match(input,REAL,FOLLOW_REAL_in_real_arg707); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.add( new Double((a1!=null?a1.getText():null))); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:162:6: p1= REAL_PATTERN
                    {
                    p1=(Token)match(input,REAL_PATTERN,FOLLOW_REAL_PATTERN_in_real_arg737); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.addAll( expandReals((p1!=null?p1.getText():null))); 
                    }

                    }
                    break;

            }

            // /Users/pcmehlitz/projects/grammars/TestSpec.g:164:4: ( '|' (a2= REAL | p2= REAL_PATTERN ) )*
            loop15:
            do {
                int alt15=2;
                int LA15_0 = input.LA(1);

                if ( (LA15_0==17) ) {
                    alt15=1;
                }


                switch (alt15) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:164:5: '|' (a2= REAL | p2= REAL_PATTERN )
            	    {
            	    match(input,17,FOLLOW_17_in_real_arg759); if (state.failed) return ;
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:164:9: (a2= REAL | p2= REAL_PATTERN )
            	    int alt14=2;
            	    int LA14_0 = input.LA(1);

            	    if ( (LA14_0==REAL) ) {
            	        alt14=1;
            	    }
            	    else if ( (LA14_0==REAL_PATTERN) ) {
            	        alt14=2;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return ;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("", 14, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt14) {
            	        case 1 :
            	            // /Users/pcmehlitz/projects/grammars/TestSpec.g:164:10: a2= REAL
            	            {
            	            a2=(Token)match(input,REAL,FOLLOW_REAL_in_real_arg765); if (state.failed) return ;
            	            if ( state.backtracking==0 ) {
            	               s.add( new Double((a2!=null?a2.getText():null))); 
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/pcmehlitz/projects/grammars/TestSpec.g:165:11: p2= REAL_PATTERN
            	            {
            	            p2=(Token)match(input,REAL_PATTERN,FOLLOW_REAL_PATTERN_in_real_arg795); if (state.failed) return ;
            	            if ( state.backtracking==0 ) {
            	               s.addAll( expandReals((p2!=null?p2.getText():null))); 
            	            }

            	            }
            	            break;

            	    }


            	    }
            	    break;

            	default :
            	    break loop15;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "real_arg"


    // $ANTLR start "string_arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:170:1: string_arg[ValSet s] : (a1= STRING | p1= STRING_PATTERN ) ( '|' (a2= STRING | p2= STRING_PATTERN ) )* ;
    public final void string_arg(ValSet s) throws RecognitionException {
        Token a1=null;
        Token p1=null;
        Token a2=null;
        Token p2=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:171:2: ( (a1= STRING | p1= STRING_PATTERN ) ( '|' (a2= STRING | p2= STRING_PATTERN ) )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:171:4: (a1= STRING | p1= STRING_PATTERN ) ( '|' (a2= STRING | p2= STRING_PATTERN ) )*
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:171:4: (a1= STRING | p1= STRING_PATTERN )
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==STRING) ) {
                alt16=1;
            }
            else if ( (LA16_0==STRING_PATTERN) ) {
                alt16=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 16, 0, input);

                throw nvae;
            }
            switch (alt16) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:171:5: a1= STRING
                    {
                    a1=(Token)match(input,STRING,FOLLOW_STRING_in_string_arg835); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.add( (a1!=null?a1.getText():null)); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:172:6: p1= STRING_PATTERN
                    {
                    p1=(Token)match(input,STRING_PATTERN,FOLLOW_STRING_PATTERN_in_string_arg863); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.addAll( expandStrings( (p1!=null?p1.getText():null))); 
                    }

                    }
                    break;

            }

            // /Users/pcmehlitz/projects/grammars/TestSpec.g:174:4: ( '|' (a2= STRING | p2= STRING_PATTERN ) )*
            loop18:
            do {
                int alt18=2;
                int LA18_0 = input.LA(1);

                if ( (LA18_0==17) ) {
                    alt18=1;
                }


                switch (alt18) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:174:6: '|' (a2= STRING | p2= STRING_PATTERN )
            	    {
            	    match(input,17,FOLLOW_17_in_string_arg884); if (state.failed) return ;
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:174:10: (a2= STRING | p2= STRING_PATTERN )
            	    int alt17=2;
            	    int LA17_0 = input.LA(1);

            	    if ( (LA17_0==STRING) ) {
            	        alt17=1;
            	    }
            	    else if ( (LA17_0==STRING_PATTERN) ) {
            	        alt17=2;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return ;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("", 17, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt17) {
            	        case 1 :
            	            // /Users/pcmehlitz/projects/grammars/TestSpec.g:174:11: a2= STRING
            	            {
            	            a2=(Token)match(input,STRING,FOLLOW_STRING_in_string_arg890); if (state.failed) return ;
            	            if ( state.backtracking==0 ) {
            	               s.add( (a2!=null?a2.getText():null)); 
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/pcmehlitz/projects/grammars/TestSpec.g:175:12: p2= STRING_PATTERN
            	            {
            	            p2=(Token)match(input,STRING_PATTERN,FOLLOW_STRING_PATTERN_in_string_arg918); if (state.failed) return ;
            	            if ( state.backtracking==0 ) {
            	               s.addAll( expandStrings( (p2!=null?p2.getText():null))); 
            	            }

            	            }
            	            break;

            	    }


            	    }
            	    break;

            	default :
            	    break loop18;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "string_arg"


    // $ANTLR start "boolean_arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:180:1: boolean_arg[ValSet s] : ( 'true' | 'false' );
    public final void boolean_arg(ValSet s) throws RecognitionException {
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:181:2: ( 'true' | 'false' )
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0==23) ) {
                alt19=1;
            }
            else if ( (LA19_0==24) ) {
                alt19=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 19, 0, input);

                throw nvae;
            }
            switch (alt19) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:181:4: 'true'
                    {
                    match(input,23,FOLLOW_23_in_boolean_arg951); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.add(Boolean.TRUE); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:182:4: 'false'
                    {
                    match(input,24,FOLLOW_24_in_boolean_arg979); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       s.add(Boolean.FALSE); 
                    }

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "boolean_arg"


    // $ANTLR start "field_arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:185:1: field_arg[ValSet s] : i1= ID ( '|' i2= ID )* ;
    public final void field_arg(ValSet s) throws RecognitionException {
        Token i1=null;
        Token i2=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:186:2: (i1= ID ( '|' i2= ID )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:186:4: i1= ID ( '|' i2= ID )*
            {
            i1=(Token)match(input,ID,FOLLOW_ID_in_field_arg1017); if (state.failed) return ;
            if ( state.backtracking==0 ) {
               s.add( new FieldReference((i1!=null?i1.getText():null))); 
            }
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:187:4: ( '|' i2= ID )*
            loop20:
            do {
                int alt20=2;
                int LA20_0 = input.LA(1);

                if ( (LA20_0==17) ) {
                    alt20=1;
                }


                switch (alt20) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:187:6: '|' i2= ID
            	    {
            	    match(input,17,FOLLOW_17_in_field_arg1047); if (state.failed) return ;
            	    i2=(Token)match(input,ID,FOLLOW_ID_in_field_arg1052); if (state.failed) return ;
            	    if ( state.backtracking==0 ) {
            	       s.add( new FieldReference((i2!=null?i2.getText():null))); 
            	    }

            	    }
            	    break;

            	default :
            	    break loop20;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "field_arg"


    // $ANTLR start "object_arg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:191:1: object_arg[ValSet s] : a1= object ( '|' a2= object )* ;
    public final void object_arg(ValSet s) throws RecognitionException {
        ValSet a1 = null;

        ValSet a2 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:192:2: (a1= object ( '|' a2= object )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:192:4: a1= object ( '|' a2= object )*
            {
            pushFollow(FOLLOW_object_in_object_arg1093);
            a1=object();

            state._fsp--;
            if (state.failed) return ;
            if ( state.backtracking==0 ) {
               s.addAll( a1.getValues()); 
            }
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:193:4: ( '|' a2= object )*
            loop21:
            do {
                int alt21=2;
                int LA21_0 = input.LA(1);

                if ( (LA21_0==17) ) {
                    alt21=1;
                }


                switch (alt21) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:193:6: '|' a2= object
            	    {
            	    match(input,17,FOLLOW_17_in_object_arg1119); if (state.failed) return ;
            	    pushFollow(FOLLOW_object_in_object_arg1124);
            	    a2=object();

            	    state._fsp--;
            	    if (state.failed) return ;
            	    if ( state.backtracking==0 ) {
            	       s.addAll( a2.getValues()); 
            	    }

            	    }
            	    break;

            	default :
            	    break loop21;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "object_arg"


    // $ANTLR start "object"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:197:1: object returns [ValSet s] : 'new' ID arglist ;
    public final ValSet object() throws RecognitionException {
        ValSet s = null;

        Token ID2=null;
        ArgList arglist3 = null;



        		s = new ValSet();
        	
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:201:5: ( 'new' ID arglist )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:201:7: 'new' ID arglist
            {
            match(input,25,FOLLOW_25_in_object1167); if (state.failed) return s;
            ID2=(Token)match(input,ID,FOLLOW_ID_in_object1169); if (state.failed) return s;
            pushFollow(FOLLOW_arglist_in_object1171);
            arglist3=arglist();

            state._fsp--;
            if (state.failed) return s;
            if ( state.backtracking==0 ) {
               s.addAll( createObjects((ID2!=null?ID2.getText():null), arglist3)); 
            }

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return s;
    }
    // $ANTLR end "object"


    // $ANTLR start "list"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:204:1: list returns [ArgList a] : '{' (a1= arg ( ',' a2= arg )* )? '}' ;
    public final ArgList list() throws RecognitionException {
        ArgList a = null;

        ValSet a1 = null;

        ValSet a2 = null;



                a = new ArgList();
            
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:208:5: ( '{' (a1= arg ( ',' a2= arg )* )? '}' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:208:7: '{' (a1= arg ( ',' a2= arg )* )? '}'
            {
            match(input,26,FOLLOW_26_in_list1214); if (state.failed) return a;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:209:9: (a1= arg ( ',' a2= arg )* )?
            int alt23=2;
            int LA23_0 = input.LA(1);

            if ( ((LA23_0>=INT && LA23_0<=ID)||(LA23_0>=23 && LA23_0<=26)) ) {
                alt23=1;
            }
            switch (alt23) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:209:10: a1= arg ( ',' a2= arg )*
                    {
                    pushFollow(FOLLOW_arg_in_list1228);
                    a1=arg();

                    state._fsp--;
                    if (state.failed) return a;
                    if ( state.backtracking==0 ) {
                       a.add(a1); 
                    }
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:210:11: ( ',' a2= arg )*
                    loop22:
                    do {
                        int alt22=2;
                        int LA22_0 = input.LA(1);

                        if ( (LA22_0==19) ) {
                            alt22=1;
                        }


                        switch (alt22) {
                    	case 1 :
                    	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:210:12: ',' a2= arg
                    	    {
                    	    match(input,19,FOLLOW_19_in_list1260); if (state.failed) return a;
                    	    pushFollow(FOLLOW_arg_in_list1265);
                    	    a2=arg();

                    	    state._fsp--;
                    	    if (state.failed) return a;
                    	    if ( state.backtracking==0 ) {
                    	       a.add(a2); 
                    	    }

                    	    }
                    	    break;

                    	default :
                    	    break loop22;
                        }
                    } while (true);


                    }
                    break;

            }

            match(input,27,FOLLOW_27_in_list1310); if (state.failed) return a;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return a;
    }
    // $ANTLR end "list"


    // $ANTLR start "num"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:216:1: num returns [Object o] : ( INT | REAL );
    public final Object num() throws RecognitionException {
        Object o = null;

        Token INT4=null;
        Token REAL5=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:217:2: ( INT | REAL )
            int alt24=2;
            int LA24_0 = input.LA(1);

            if ( (LA24_0==INT) ) {
                alt24=1;
            }
            else if ( (LA24_0==REAL) ) {
                alt24=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return o;}
                NoViableAltException nvae =
                    new NoViableAltException("", 24, 0, input);

                throw nvae;
            }
            switch (alt24) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:217:4: INT
                    {
                    INT4=(Token)match(input,INT,FOLLOW_INT_in_num1328); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Integer((INT4!=null?INT4.getText():null)); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:218:4: REAL
                    {
                    REAL5=(Token)match(input,REAL,FOLLOW_REAL_in_num1359); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Double((REAL5!=null?REAL5.getText():null)); 
                    }

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return o;
    }
    // $ANTLR end "num"


    // $ANTLR start "goal"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:221:1: goal returns [Goal g] : ( compareGoal | matchGoal | withinGoal | throwsGoal | noThrowsGoal | satisfiesGoal );
    public final Goal goal() throws RecognitionException {
        Goal g = null;

        Goal compareGoal6 = null;

        Goal matchGoal7 = null;

        Goal withinGoal8 = null;

        Goal throwsGoal9 = null;

        Goal noThrowsGoal10 = null;

        Goal satisfiesGoal11 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:222:2: ( compareGoal | matchGoal | withinGoal | throwsGoal | noThrowsGoal | satisfiesGoal )
            int alt25=6;
            switch ( input.LA(1) ) {
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
                {
                alt25=1;
                }
                break;
            case 34:
                {
                alt25=2;
                }
                break;
            case 35:
                {
                alt25=3;
                }
                break;
            case 37:
                {
                alt25=4;
                }
                break;
            case 38:
                {
                alt25=5;
                }
                break;
            case 39:
                {
                alt25=6;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return g;}
                NoViableAltException nvae =
                    new NoViableAltException("", 25, 0, input);

                throw nvae;
            }

            switch (alt25) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:222:4: compareGoal
                    {
                    pushFollow(FOLLOW_compareGoal_in_goal1399);
                    compareGoal6=compareGoal();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = compareGoal6; 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:223:4: matchGoal
                    {
                    pushFollow(FOLLOW_matchGoal_in_goal1422);
                    matchGoal7=matchGoal();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = matchGoal7; 
                    }

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:224:4: withinGoal
                    {
                    pushFollow(FOLLOW_withinGoal_in_goal1456);
                    withinGoal8=withinGoal();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = withinGoal8; 
                    }

                    }
                    break;
                case 4 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:225:4: throwsGoal
                    {
                    pushFollow(FOLLOW_throwsGoal_in_goal1480);
                    throwsGoal9=throwsGoal();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = throwsGoal9; 
                    }

                    }
                    break;
                case 5 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:226:4: noThrowsGoal
                    {
                    pushFollow(FOLLOW_noThrowsGoal_in_goal1504);
                    noThrowsGoal10=noThrowsGoal();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = noThrowsGoal10; 
                    }

                    }
                    break;
                case 6 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:227:4: satisfiesGoal
                    {
                    pushFollow(FOLLOW_satisfiesGoal_in_goal1526);
                    satisfiesGoal11=satisfiesGoal();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = satisfiesGoal11; 
                    }

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return g;
    }
    // $ANTLR end "goal"


    // $ANTLR start "compareGoal"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:232:1: compareGoal returns [Goal g] : ( '==' a1= arg | '!=' a2= arg | '<' a3= arg | '<=' a4= arg | '>' a5= arg | '>=' a6= arg );
    public final Goal compareGoal() throws RecognitionException {
        Goal g = null;

        ValSet a1 = null;

        ValSet a2 = null;

        ValSet a3 = null;

        ValSet a4 = null;

        ValSet a5 = null;

        ValSet a6 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:233:2: ( '==' a1= arg | '!=' a2= arg | '<' a3= arg | '<=' a4= arg | '>' a5= arg | '>=' a6= arg )
            int alt26=6;
            switch ( input.LA(1) ) {
            case 28:
                {
                alt26=1;
                }
                break;
            case 29:
                {
                alt26=2;
                }
                break;
            case 30:
                {
                alt26=3;
                }
                break;
            case 31:
                {
                alt26=4;
                }
                break;
            case 32:
                {
                alt26=5;
                }
                break;
            case 33:
                {
                alt26=6;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return g;}
                NoViableAltException nvae =
                    new NoViableAltException("", 26, 0, input);

                throw nvae;
            }

            switch (alt26) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:233:4: '==' a1= arg
                    {
                    match(input,28,FOLLOW_28_in_compareGoal1560); if (state.failed) return g;
                    pushFollow(FOLLOW_arg_in_compareGoal1564);
                    a1=arg();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new CompareGoal(CompareGoal.Operator.EQ, a1); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:234:4: '!=' a2= arg
                    {
                    match(input,29,FOLLOW_29_in_compareGoal1587); if (state.failed) return g;
                    pushFollow(FOLLOW_arg_in_compareGoal1591);
                    a2=arg();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new CompareGoal(CompareGoal.Operator.NE, a2); 
                    }

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:235:4: '<' a3= arg
                    {
                    match(input,30,FOLLOW_30_in_compareGoal1614); if (state.failed) return g;
                    pushFollow(FOLLOW_arg_in_compareGoal1619);
                    a3=arg();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new CompareGoal(CompareGoal.Operator.LT, a3); 
                    }

                    }
                    break;
                case 4 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:236:4: '<=' a4= arg
                    {
                    match(input,31,FOLLOW_31_in_compareGoal1642); if (state.failed) return g;
                    pushFollow(FOLLOW_arg_in_compareGoal1646);
                    a4=arg();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new CompareGoal(CompareGoal.Operator.LE, a4); 
                    }

                    }
                    break;
                case 5 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:237:4: '>' a5= arg
                    {
                    match(input,32,FOLLOW_32_in_compareGoal1669); if (state.failed) return g;
                    pushFollow(FOLLOW_arg_in_compareGoal1674);
                    a5=arg();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new CompareGoal(CompareGoal.Operator.GT, a5); 
                    }

                    }
                    break;
                case 6 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:238:4: '>=' a6= arg
                    {
                    match(input,33,FOLLOW_33_in_compareGoal1697); if (state.failed) return g;
                    pushFollow(FOLLOW_arg_in_compareGoal1701);
                    a6=arg();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new CompareGoal(CompareGoal.Operator.GE, a6); 
                    }

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return g;
    }
    // $ANTLR end "compareGoal"


    // $ANTLR start "matchGoal"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:241:1: matchGoal returns [Goal g] : 'matches' STRING ;
    public final Goal matchGoal() throws RecognitionException {
        Goal g = null;

        Token STRING12=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:242:2: ( 'matches' STRING )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:242:4: 'matches' STRING
            {
            match(input,34,FOLLOW_34_in_matchGoal1735); if (state.failed) return g;
            STRING12=(Token)match(input,STRING,FOLLOW_STRING_in_matchGoal1737); if (state.failed) return g;
            if ( state.backtracking==0 ) {
               g = new RegexGoal( (STRING12!=null?STRING12.getText():null)); 
            }

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return g;
    }
    // $ANTLR end "matchGoal"


    // $ANTLR start "withinGoal"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:245:1: withinGoal returns [Goal g] : 'within' a1= intervalArg ( ',' | '+-' ) a2= intervalArg ;
    public final Goal withinGoal() throws RecognitionException {
        Goal g = null;

        Object a1 = null;

        Object a2 = null;



            	boolean isDelta=false;
            
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:249:2: ( 'within' a1= intervalArg ( ',' | '+-' ) a2= intervalArg )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:249:4: 'within' a1= intervalArg ( ',' | '+-' ) a2= intervalArg
            {
            match(input,35,FOLLOW_35_in_withinGoal1776); if (state.failed) return g;
            pushFollow(FOLLOW_intervalArg_in_withinGoal1784);
            a1=intervalArg();

            state._fsp--;
            if (state.failed) return g;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:251:4: ( ',' | '+-' )
            int alt27=2;
            int LA27_0 = input.LA(1);

            if ( (LA27_0==19) ) {
                alt27=1;
            }
            else if ( (LA27_0==36) ) {
                alt27=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return g;}
                NoViableAltException nvae =
                    new NoViableAltException("", 27, 0, input);

                throw nvae;
            }
            switch (alt27) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:251:5: ','
                    {
                    match(input,19,FOLLOW_19_in_withinGoal1791); if (state.failed) return g;

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:252:5: '+-'
                    {
                    match(input,36,FOLLOW_36_in_withinGoal1797); if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       isDelta = true; 
                    }

                    }
                    break;

            }

            pushFollow(FOLLOW_intervalArg_in_withinGoal1834);
            a2=intervalArg();

            state._fsp--;
            if (state.failed) return g;
            if ( state.backtracking==0 ) {
               g = new WithinGoal( a1, a2, isDelta); 
            }

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return g;
    }
    // $ANTLR end "withinGoal"


    // $ANTLR start "intervalArg"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:257:1: intervalArg returns [Object o] : ( INT | REAL | ID );
    public final Object intervalArg() throws RecognitionException {
        Object o = null;

        Token INT13=null;
        Token REAL14=null;
        Token ID15=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:258:2: ( INT | REAL | ID )
            int alt28=3;
            switch ( input.LA(1) ) {
            case INT:
                {
                alt28=1;
                }
                break;
            case REAL:
                {
                alt28=2;
                }
                break;
            case ID:
                {
                alt28=3;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return o;}
                NoViableAltException nvae =
                    new NoViableAltException("", 28, 0, input);

                throw nvae;
            }

            switch (alt28) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:258:4: INT
                    {
                    INT13=(Token)match(input,INT,FOLLOW_INT_in_intervalArg1864); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Integer((INT13!=null?INT13.getText():null)); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:259:4: REAL
                    {
                    REAL14=(Token)match(input,REAL,FOLLOW_REAL_in_intervalArg1895); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Double((REAL14!=null?REAL14.getText():null)); 
                    }

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:260:4: ID
                    {
                    ID15=(Token)match(input,ID,FOLLOW_ID_in_intervalArg1925); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new FieldReference( (ID15!=null?ID15.getText():null)); 
                    }

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return o;
    }
    // $ANTLR end "intervalArg"


    // $ANTLR start "throwsGoal"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:264:1: throwsGoal returns [Goal g] : 'throws' ( ID | ID_PATTERN ) ;
    public final Goal throwsGoal() throws RecognitionException {
        Goal g = null;

        Token ID16=null;
        Token ID_PATTERN17=null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:265:2: ( 'throws' ( ID | ID_PATTERN ) )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:265:4: 'throws' ( ID | ID_PATTERN )
            {
            match(input,37,FOLLOW_37_in_throwsGoal1971); if (state.failed) return g;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:266:5: ( ID | ID_PATTERN )
            int alt29=2;
            int LA29_0 = input.LA(1);

            if ( (LA29_0==ID) ) {
                alt29=1;
            }
            else if ( (LA29_0==ID_PATTERN) ) {
                alt29=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return g;}
                NoViableAltException nvae =
                    new NoViableAltException("", 29, 0, input);

                throw nvae;
            }
            switch (alt29) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:266:7: ID
                    {
                    ID16=(Token)match(input,ID,FOLLOW_ID_in_throwsGoal1980); if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new ThrowsGoal( (ID16!=null?ID16.getText():null)); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:267:7: ID_PATTERN
                    {
                    ID_PATTERN17=(Token)match(input,ID_PATTERN,FOLLOW_ID_PATTERN_in_throwsGoal2012); if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       g = new ThrowsGoal( (ID_PATTERN17!=null?ID_PATTERN17.getText():null)); 
                    }

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return g;
    }
    // $ANTLR end "throwsGoal"


    // $ANTLR start "noThrowsGoal"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:271:1: noThrowsGoal returns [Goal g] : 'noThrows' ;
    public final Goal noThrowsGoal() throws RecognitionException {
        Goal g = null;

        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:272:2: ( 'noThrows' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:272:4: 'noThrows'
            {
            match(input,38,FOLLOW_38_in_noThrowsGoal2051); if (state.failed) return g;
            if ( state.backtracking==0 ) {
               g = new NoThrowsGoal(); 
            }

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return g;
    }
    // $ANTLR end "noThrowsGoal"


    // $ANTLR start "satisfiesGoal"
    // /Users/pcmehlitz/projects/grammars/TestSpec.g:276:1: satisfiesGoal returns [Goal g] : 'satisfies' ID ( arglist )? ;
    public final Goal satisfiesGoal() throws RecognitionException {
        Goal g = null;

        Token ID19=null;
        ArgList arglist18 = null;



        		ArgList a = null;
        	
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:280:2: ( 'satisfies' ID ( arglist )? )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:280:4: 'satisfies' ID ( arglist )?
            {
            match(input,39,FOLLOW_39_in_satisfiesGoal2093); if (state.failed) return g;
            ID19=(Token)match(input,ID,FOLLOW_ID_in_satisfiesGoal2095); if (state.failed) return g;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:281:4: ( arglist )?
            int alt30=2;
            int LA30_0 = input.LA(1);

            if ( (LA30_0==21) ) {
                alt30=1;
            }
            switch (alt30) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:281:5: arglist
                    {
                    pushFollow(FOLLOW_arglist_in_satisfiesGoal2101);
                    arglist18=arglist();

                    state._fsp--;
                    if (state.failed) return g;
                    if ( state.backtracking==0 ) {
                       a = arglist18; 
                    }

                    }
                    break;

            }

            if ( state.backtracking==0 ) {
               g = createGoal((ID19!=null?ID19.getText():null), a); 
            }

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return g;
    }
    // $ANTLR end "satisfiesGoal"

    // Delegated rules


 

    public static final BitSet FOLLOW_env_in_testspec73 = new BitSet(new long[]{0x0000000000060000L});
    public static final BitSet FOLLOW_17_in_testspec97 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_env_in_testspec102 = new BitSet(new long[]{0x0000000000060000L});
    public static final BitSet FOLLOW_18_in_testspec132 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_arglist_in_testspec146 = new BitSet(new long[]{0x000000EFF0020002L});
    public static final BitSet FOLLOW_17_in_testspec170 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_arglist_in_testspec175 = new BitSet(new long[]{0x000000EFF0020002L});
    public static final BitSet FOLLOW_goal_in_testspec203 = new BitSet(new long[]{0x0000000000080002L});
    public static final BitSet FOLLOW_19_in_testspec231 = new BitSet(new long[]{0x000000EFF0000000L});
    public static final BitSet FOLLOW_goal_in_testspec236 = new BitSet(new long[]{0x0000000000080002L});
    public static final BitSet FOLLOW_20_in_env281 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_arglist_in_env283 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_21_in_arglist324 = new BitSet(new long[]{0x0000000007C007F0L});
    public static final BitSet FOLLOW_arg_in_arglist336 = new BitSet(new long[]{0x0000000000480000L});
    public static final BitSet FOLLOW_19_in_arglist366 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_arglist371 = new BitSet(new long[]{0x0000000000480000L});
    public static final BitSet FOLLOW_22_in_arglist408 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_int_arg_in_arg431 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_real_arg_in_arg438 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_string_arg_in_arg445 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolean_arg_in_arg452 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_object_arg_in_arg459 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_field_arg_in_arg466 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_list_arg_in_arg473 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_list_in_list_arg495 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_17_in_list_arg525 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_list_in_list_arg530 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_INT_in_int_arg575 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_INT_PATTERN_in_int_arg606 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_17_in_int_arg629 = new BitSet(new long[]{0x0000000000000030L});
    public static final BitSet FOLLOW_INT_in_int_arg635 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_INT_PATTERN_in_int_arg666 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_REAL_in_real_arg707 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_REAL_PATTERN_in_real_arg737 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_17_in_real_arg759 = new BitSet(new long[]{0x00000000000000C0L});
    public static final BitSet FOLLOW_REAL_in_real_arg765 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_REAL_PATTERN_in_real_arg795 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_STRING_in_string_arg835 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_STRING_PATTERN_in_string_arg863 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_17_in_string_arg884 = new BitSet(new long[]{0x0000000000000300L});
    public static final BitSet FOLLOW_STRING_in_string_arg890 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_STRING_PATTERN_in_string_arg918 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_23_in_boolean_arg951 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_24_in_boolean_arg979 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_field_arg1017 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_17_in_field_arg1047 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_ID_in_field_arg1052 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_object_in_object_arg1093 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_17_in_object_arg1119 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_object_in_object_arg1124 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_25_in_object1167 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_ID_in_object1169 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_arglist_in_object1171 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_list1214 = new BitSet(new long[]{0x000000000F8007F0L});
    public static final BitSet FOLLOW_arg_in_list1228 = new BitSet(new long[]{0x0000000008080000L});
    public static final BitSet FOLLOW_19_in_list1260 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_list1265 = new BitSet(new long[]{0x0000000008080000L});
    public static final BitSet FOLLOW_27_in_list1310 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_num1328 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REAL_in_num1359 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_compareGoal_in_goal1399 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_matchGoal_in_goal1422 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_withinGoal_in_goal1456 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_throwsGoal_in_goal1480 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_noThrowsGoal_in_goal1504 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_satisfiesGoal_in_goal1526 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_compareGoal1560 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_compareGoal1564 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_compareGoal1587 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_compareGoal1591 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_compareGoal1614 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_compareGoal1619 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_compareGoal1642 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_compareGoal1646 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_compareGoal1669 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_compareGoal1674 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_compareGoal1697 = new BitSet(new long[]{0x00000000078007F0L});
    public static final BitSet FOLLOW_arg_in_compareGoal1701 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_matchGoal1735 = new BitSet(new long[]{0x0000000000000100L});
    public static final BitSet FOLLOW_STRING_in_matchGoal1737 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_withinGoal1776 = new BitSet(new long[]{0x0000000000000450L});
    public static final BitSet FOLLOW_intervalArg_in_withinGoal1784 = new BitSet(new long[]{0x0000001000080000L});
    public static final BitSet FOLLOW_19_in_withinGoal1791 = new BitSet(new long[]{0x0000000000000450L});
    public static final BitSet FOLLOW_36_in_withinGoal1797 = new BitSet(new long[]{0x0000000000000450L});
    public static final BitSet FOLLOW_intervalArg_in_withinGoal1834 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_intervalArg1864 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REAL_in_intervalArg1895 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_intervalArg1925 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_throwsGoal1971 = new BitSet(new long[]{0x0000000000000C00L});
    public static final BitSet FOLLOW_ID_in_throwsGoal1980 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_PATTERN_in_throwsGoal2012 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_noThrowsGoal2051 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_satisfiesGoal2093 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_ID_in_satisfiesGoal2095 = new BitSet(new long[]{0x0000000000200002L});
    public static final BitSet FOLLOW_arglist_in_satisfiesGoal2101 = new BitSet(new long[]{0x0000000000000002L});

}