// $ANTLR 3.1.2 /Users/pcmehlitz/projects/grammars/ContractSpec.g 2009-05-05 15:56:37

	package gov.nasa.jpf.verify;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
public class ContractSpecParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "ID", "STRING", "INT", "REAL", "SIGN", "NUM", "ESCAPE", "STRING_PATTERN", "WS", "'||'", "'&&'", "'('", "')'", "'=='", "'!='", "'>'", "'>='", "'<'", "'<='", "'within'", "','", "'+-'", "'isEmpty'", "'notEmpty'", "'instanceof'", "'matches'", "'satisfies'", "'+'", "'-'", "'*'", "'/'", "'log10'", "'log'", "'^'", "'null'", "'return'", "'EPS'", "'old'"
    };
    public static final int SIGN=8;
    public static final int T__40=40;
    public static final int T__41=41;
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
    public static final int INT=6;
    public static final int ID=4;
    public static final int EOF=-1;
    public static final int NUM=9;
    public static final int STRING_PATTERN=11;
    public static final int REAL=7;
    public static final int T__30=30;
    public static final int T__19=19;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int WS=12;
    public static final int T__33=33;
    public static final int T__16=16;
    public static final int T__34=34;
    public static final int T__15=15;
    public static final int ESCAPE=10;
    public static final int T__35=35;
    public static final int T__18=18;
    public static final int T__36=36;
    public static final int T__17=17;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int T__14=14;
    public static final int T__13=13;
    public static final int STRING=5;

    // delegates
    // delegators


        public ContractSpecParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public ContractSpecParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return ContractSpecParser.tokenNames; }
    public String getGrammarFileName() { return "/Users/pcmehlitz/projects/grammars/ContractSpec.g"; }


        ContractContext ctx;
        
        public ContractSpecParser (TokenStream input, ContractContext ctx) {
          this(input);
          
          this.ctx = ctx;
        }



    // $ANTLR start "contractSpec"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:27:1: contractSpec : contract ;
    public final void contractSpec() throws RecognitionException {
        Contract contract1 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:31:5: ( contract )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:32:5: contract
            {
            pushFollow(FOLLOW_contract_in_contractSpec64);
            contract1=contract();

            state._fsp--;
            if (state.failed) return ;
            if ( state.backtracking==0 ) {
               System.out.println(contract1); 
            }

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
    // $ANTLR end "contractSpec"


    // $ANTLR start "contract"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:35:1: contract returns [Contract c] : c1= contractAnd ( '||' c2= contractAnd )* ;
    public final Contract contract() throws RecognitionException {
        Contract c = null;

        Contract c1 = null;

        Contract c2 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:36:5: (c1= contractAnd ( '||' c2= contractAnd )* )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:37:5: c1= contractAnd ( '||' c2= contractAnd )*
            {
            pushFollow(FOLLOW_contractAnd_in_contract114);
            c1=contractAnd();

            state._fsp--;
            if (state.failed) return c;
            if ( state.backtracking==0 ) {
               c=c1; 
            }
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:38:5: ( '||' c2= contractAnd )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==13) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:38:6: '||' c2= contractAnd
            	    {
            	    match(input,13,FOLLOW_13_in_contract141); if (state.failed) return c;
            	    pushFollow(FOLLOW_contractAnd_in_contract145);
            	    c2=contractAnd();

            	    state._fsp--;
            	    if (state.failed) return c;
            	    if ( state.backtracking==0 ) {
            	       c= new ContractOr(c,c2); 
            	    }

            	    }
            	    break;

            	default :
            	    break loop1;
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
        return c;
    }
    // $ANTLR end "contract"


    // $ANTLR start "contractAnd"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:42:1: contractAnd returns [Contract c] : c1= contractAtom ( '&&' c2= contractAtom )* ;
    public final Contract contractAnd() throws RecognitionException {
        Contract c = null;

        Contract c1 = null;

        Contract c2 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:43:5: (c1= contractAtom ( '&&' c2= contractAtom )* )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:44:5: c1= contractAtom ( '&&' c2= contractAtom )*
            {
            pushFollow(FOLLOW_contractAtom_in_contractAnd194);
            c1=contractAtom();

            state._fsp--;
            if (state.failed) return c;
            if ( state.backtracking==0 ) {
               c=c1; 
            }
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:45:5: ( '&&' c2= contractAtom )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==14) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:45:6: '&&' c2= contractAtom
            	    {
            	    match(input,14,FOLLOW_14_in_contractAnd220); if (state.failed) return c;
            	    pushFollow(FOLLOW_contractAtom_in_contractAnd224);
            	    c2=contractAtom();

            	    state._fsp--;
            	    if (state.failed) return c;
            	    if ( state.backtracking==0 ) {
            	       c= new ContractAnd(c,c2); 
            	    }

            	    }
            	    break;

            	default :
            	    break loop2;
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
        return c;
    }
    // $ANTLR end "contractAnd"


    // $ANTLR start "contractAtom"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:49:1: contractAtom returns [Contract c] : ( simpleContract | '(' contract ')' );
    public final Contract contractAtom() throws RecognitionException {
        Contract c = null;

        Contract simpleContract2 = null;

        Contract contract3 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:50:5: ( simpleContract | '(' contract ')' )
            int alt3=2;
            alt3 = dfa3.predict(input);
            switch (alt3) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:51:2: simpleContract
                    {
                    pushFollow(FOLLOW_simpleContract_in_contractAtom264);
                    simpleContract2=simpleContract();

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c= simpleContract2; 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:52:4: '(' contract ')'
                    {
                    match(input,15,FOLLOW_15_in_contractAtom289); if (state.failed) return c;
                    pushFollow(FOLLOW_contract_in_contractAtom291);
                    contract3=contract();

                    state._fsp--;
                    if (state.failed) return c;
                    match(input,16,FOLLOW_16_in_contractAtom293); if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c= contract3; 
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
        return c;
    }
    // $ANTLR end "contractAtom"


    // $ANTLR start "simpleContract"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:55:1: simpleContract returns [Contract c] : e1= expr ( '==' e2= expr | '!=' e3= expr | '>' e4= expr | '>=' e5= expr | '<' e6= expr | '<=' e7= expr | 'within' e8= expr ( ',' e9= expr | '+-' e10= expr ) | 'isEmpty' | 'notEmpty' | 'instanceof' ID | 'matches' STRING | genericContract[$e1.o] ) ;
    public final Contract simpleContract() throws RecognitionException {
        Contract c = null;

        Token ID4=null;
        Token STRING5=null;
        Operand e1 = null;

        Operand e2 = null;

        Operand e3 = null;

        Operand e4 = null;

        Operand e5 = null;

        Operand e6 = null;

        Operand e7 = null;

        Operand e8 = null;

        Operand e9 = null;

        Operand e10 = null;

        Contract genericContract6 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:56:2: (e1= expr ( '==' e2= expr | '!=' e3= expr | '>' e4= expr | '>=' e5= expr | '<' e6= expr | '<=' e7= expr | 'within' e8= expr ( ',' e9= expr | '+-' e10= expr ) | 'isEmpty' | 'notEmpty' | 'instanceof' ID | 'matches' STRING | genericContract[$e1.o] ) )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:57:2: e1= expr ( '==' e2= expr | '!=' e3= expr | '>' e4= expr | '>=' e5= expr | '<' e6= expr | '<=' e7= expr | 'within' e8= expr ( ',' e9= expr | '+-' e10= expr ) | 'isEmpty' | 'notEmpty' | 'instanceof' ID | 'matches' STRING | genericContract[$e1.o] )
            {
            pushFollow(FOLLOW_expr_in_simpleContract328);
            e1=expr();

            state._fsp--;
            if (state.failed) return c;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:58:2: ( '==' e2= expr | '!=' e3= expr | '>' e4= expr | '>=' e5= expr | '<' e6= expr | '<=' e7= expr | 'within' e8= expr ( ',' e9= expr | '+-' e10= expr ) | 'isEmpty' | 'notEmpty' | 'instanceof' ID | 'matches' STRING | genericContract[$e1.o] )
            int alt5=12;
            switch ( input.LA(1) ) {
            case 17:
                {
                alt5=1;
                }
                break;
            case 18:
                {
                alt5=2;
                }
                break;
            case 19:
                {
                alt5=3;
                }
                break;
            case 20:
                {
                alt5=4;
                }
                break;
            case 21:
                {
                alt5=5;
                }
                break;
            case 22:
                {
                alt5=6;
                }
                break;
            case 23:
                {
                alt5=7;
                }
                break;
            case 26:
                {
                alt5=8;
                }
                break;
            case 27:
                {
                alt5=9;
                }
                break;
            case 28:
                {
                alt5=10;
                }
                break;
            case 29:
                {
                alt5=11;
                }
                break;
            case 30:
                {
                alt5=12;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return c;}
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;
            }

            switch (alt5) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:59:2: '==' e2= expr
                    {
                    match(input,17,FOLLOW_17_in_simpleContract334); if (state.failed) return c;
                    pushFollow(FOLLOW_expr_in_simpleContract338);
                    e2=expr();

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.EQ(e1, e2); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:60:4: '!=' e3= expr
                    {
                    match(input,18,FOLLOW_18_in_simpleContract366); if (state.failed) return c;
                    pushFollow(FOLLOW_expr_in_simpleContract370);
                    e3=expr();

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.NE(e1, e3); 
                    }

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:61:4: '>' e4= expr
                    {
                    match(input,19,FOLLOW_19_in_simpleContract395); if (state.failed) return c;
                    pushFollow(FOLLOW_expr_in_simpleContract399);
                    e4=expr();

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.GT(e1, e4); 
                    }

                    }
                    break;
                case 4 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:62:4: '>=' e5= expr
                    {
                    match(input,20,FOLLOW_20_in_simpleContract425); if (state.failed) return c;
                    pushFollow(FOLLOW_expr_in_simpleContract429);
                    e5=expr();

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.GE(e1, e5); 
                    }

                    }
                    break;
                case 5 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:63:4: '<' e6= expr
                    {
                    match(input,21,FOLLOW_21_in_simpleContract454); if (state.failed) return c;
                    pushFollow(FOLLOW_expr_in_simpleContract458);
                    e6=expr();

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.LT(e1, e6); 
                    }

                    }
                    break;
                case 6 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:64:4: '<=' e7= expr
                    {
                    match(input,22,FOLLOW_22_in_simpleContract484); if (state.failed) return c;
                    pushFollow(FOLLOW_expr_in_simpleContract488);
                    e7=expr();

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.LE(e1, e7); 
                    }

                    }
                    break;
                case 7 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:65:4: 'within' e8= expr ( ',' e9= expr | '+-' e10= expr )
                    {
                    match(input,23,FOLLOW_23_in_simpleContract513); if (state.failed) return c;
                    pushFollow(FOLLOW_expr_in_simpleContract517);
                    e8=expr();

                    state._fsp--;
                    if (state.failed) return c;
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:66:6: ( ',' e9= expr | '+-' e10= expr )
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0==24) ) {
                        alt4=1;
                    }
                    else if ( (LA4_0==25) ) {
                        alt4=2;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return c;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 4, 0, input);

                        throw nvae;
                    }
                    switch (alt4) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:66:8: ',' e9= expr
                            {
                            match(input,24,FOLLOW_24_in_simpleContract527); if (state.failed) return c;
                            pushFollow(FOLLOW_expr_in_simpleContract531);
                            e9=expr();

                            state._fsp--;
                            if (state.failed) return c;
                            if ( state.backtracking==0 ) {
                               c = new Contract.Within( e1, e8, e9); 
                            }

                            }
                            break;
                        case 2 :
                            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:67:8: '+-' e10= expr
                            {
                            match(input,25,FOLLOW_25_in_simpleContract557); if (state.failed) return c;
                            pushFollow(FOLLOW_expr_in_simpleContract561);
                            e10=expr();

                            state._fsp--;
                            if (state.failed) return c;
                            if ( state.backtracking==0 ) {
                               c = new Contract.WithinCenter( e1, e8, e10); 
                            }

                            }
                            break;

                    }


                    }
                    break;
                case 8 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:69:4: 'isEmpty'
                    {
                    match(input,26,FOLLOW_26_in_simpleContract588); if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.IsEmpty( e1); 
                    }

                    }
                    break;
                case 9 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:70:7: 'notEmpty'
                    {
                    match(input,27,FOLLOW_27_in_simpleContract619); if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.NotEmpty( e1); 
                    }

                    }
                    break;
                case 10 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:71:7: 'instanceof' ID
                    {
                    match(input,28,FOLLOW_28_in_simpleContract649); if (state.failed) return c;
                    ID4=(Token)match(input,ID,FOLLOW_ID_in_simpleContract651); if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.InstanceOf( e1, (ID4!=null?ID4.getText():null)); 
                    }

                    }
                    break;
                case 11 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:72:7: 'matches' STRING
                    {
                    match(input,29,FOLLOW_29_in_simpleContract676); if (state.failed) return c;
                    STRING5=(Token)match(input,STRING,FOLLOW_STRING_in_simpleContract678); if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = new Contract.Matches( e1, (STRING5!=null?STRING5.getText():null)); 
                    }

                    }
                    break;
                case 12 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:73:7: genericContract[$e1.o]
                    {
                    pushFollow(FOLLOW_genericContract_in_simpleContract702);
                    genericContract6=genericContract(e1);

                    state._fsp--;
                    if (state.failed) return c;
                    if ( state.backtracking==0 ) {
                       c = genericContract6; 
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
        return c;
    }
    // $ANTLR end "simpleContract"


    // $ANTLR start "genericContract"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:77:1: genericContract[Operand o] returns [Contract c] : 'satisfies' ID ( '(' (o1= expr ( ',' o2= expr )* )? ')' )? ;
    public final Contract genericContract(Operand o) throws RecognitionException {
        Contract c = null;

        Token ID7=null;
        Operand o1 = null;

        Operand o2 = null;



            	ArrayList<Operand> args = null;
            	String id = null;
            
        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:85:2: ( 'satisfies' ID ( '(' (o1= expr ( ',' o2= expr )* )? ')' )? )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:86:3: 'satisfies' ID ( '(' (o1= expr ( ',' o2= expr )* )? ')' )?
            {
            match(input,30,FOLLOW_30_in_genericContract754); if (state.failed) return c;
            ID7=(Token)match(input,ID,FOLLOW_ID_in_genericContract756); if (state.failed) return c;
            if ( state.backtracking==0 ) {
               id=(ID7!=null?ID7.getText():null); 
            }
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:87:4: ( '(' (o1= expr ( ',' o2= expr )* )? ')' )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==15) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:87:5: '(' (o1= expr ( ',' o2= expr )* )? ')'
                    {
                    match(input,15,FOLLOW_15_in_genericContract781); if (state.failed) return c;
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:88:8: (o1= expr ( ',' o2= expr )* )?
                    int alt7=2;
                    int LA7_0 = input.LA(1);

                    if ( ((LA7_0>=ID && LA7_0<=REAL)||LA7_0==15||LA7_0==32||(LA7_0>=35 && LA7_0<=36)||(LA7_0>=38 && LA7_0<=41)) ) {
                        alt7=1;
                    }
                    switch (alt7) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:88:9: o1= expr ( ',' o2= expr )*
                            {
                            pushFollow(FOLLOW_expr_in_genericContract820);
                            o1=expr();

                            state._fsp--;
                            if (state.failed) return c;
                            if ( state.backtracking==0 ) {
                               args = new ArrayList<Operand>(); args.add(o1); 
                            }
                            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:89:9: ( ',' o2= expr )*
                            loop6:
                            do {
                                int alt6=2;
                                int LA6_0 = input.LA(1);

                                if ( (LA6_0==24) ) {
                                    alt6=1;
                                }


                                switch (alt6) {
                            	case 1 :
                            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:89:10: ',' o2= expr
                            	    {
                            	    match(input,24,FOLLOW_24_in_genericContract851); if (state.failed) return c;
                            	    pushFollow(FOLLOW_expr_in_genericContract855);
                            	    o2=expr();

                            	    state._fsp--;
                            	    if (state.failed) return c;
                            	    if ( state.backtracking==0 ) {
                            	       args.add(o2); 
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

                    match(input,16,FOLLOW_16_in_genericContract897); if (state.failed) return c;

                    }
                    break;

            }


            }

            if ( state.backtracking==0 ) {

                      c = new Satisfies(ctx, id, o, args);
                  
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return c;
    }
    // $ANTLR end "genericContract"


    // $ANTLR start "expr"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:97:1: expr returns [Operand o] : o1= mult ( '+' o2= mult | '-' o3= mult )* ;
    public final Operand expr() throws RecognitionException {
        Operand o = null;

        Operand o1 = null;

        Operand o2 = null;

        Operand o3 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:98:5: (o1= mult ( '+' o2= mult | '-' o3= mult )* )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:99:5: o1= mult ( '+' o2= mult | '-' o3= mult )*
            {
            pushFollow(FOLLOW_mult_in_expr955);
            o1=mult();

            state._fsp--;
            if (state.failed) return o;
            if ( state.backtracking==0 ) {
               o = o1; 
            }
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:100:5: ( '+' o2= mult | '-' o3= mult )*
            loop9:
            do {
                int alt9=3;
                int LA9_0 = input.LA(1);

                if ( (LA9_0==31) ) {
                    alt9=1;
                }
                else if ( (LA9_0==32) ) {
                    alt9=2;
                }


                switch (alt9) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:100:7: '+' o2= mult
            	    {
            	    match(input,31,FOLLOW_31_in_expr990); if (state.failed) return o;
            	    pushFollow(FOLLOW_mult_in_expr994);
            	    o2=mult();

            	    state._fsp--;
            	    if (state.failed) return o;
            	    if ( state.backtracking==0 ) {
            	       o = new Expr.Plus(o, o2); 
            	    }

            	    }
            	    break;
            	case 2 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:101:7: '-' o3= mult
            	    {
            	    match(input,32,FOLLOW_32_in_expr1023); if (state.failed) return o;
            	    pushFollow(FOLLOW_mult_in_expr1027);
            	    o3=mult();

            	    state._fsp--;
            	    if (state.failed) return o;
            	    if ( state.backtracking==0 ) {
            	       o = new Expr.Minus(o, o3); 
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
        return o;
    }
    // $ANTLR end "expr"


    // $ANTLR start "mult"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:105:1: mult returns [Operand o] : o1= log ( '*' o2= log | '/' o3= log )* ;
    public final Operand mult() throws RecognitionException {
        Operand o = null;

        Operand o1 = null;

        Operand o2 = null;

        Operand o3 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:106:5: (o1= log ( '*' o2= log | '/' o3= log )* )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:107:2: o1= log ( '*' o2= log | '/' o3= log )*
            {
            pushFollow(FOLLOW_log_in_mult1085);
            o1=log();

            state._fsp--;
            if (state.failed) return o;
            if ( state.backtracking==0 ) {
               o = o1; 
            }
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:108:2: ( '*' o2= log | '/' o3= log )*
            loop10:
            do {
                int alt10=3;
                int LA10_0 = input.LA(1);

                if ( (LA10_0==33) ) {
                    alt10=1;
                }
                else if ( (LA10_0==34) ) {
                    alt10=2;
                }


                switch (alt10) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:108:4: '*' o2= log
            	    {
            	    match(input,33,FOLLOW_33_in_mult1118); if (state.failed) return o;
            	    pushFollow(FOLLOW_log_in_mult1122);
            	    o2=log();

            	    state._fsp--;
            	    if (state.failed) return o;
            	    if ( state.backtracking==0 ) {
            	       o = new Expr.Mult(o,o2); 
            	    }

            	    }
            	    break;
            	case 2 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:109:7: '/' o3= log
            	    {
            	    match(input,34,FOLLOW_34_in_mult1152); if (state.failed) return o;
            	    pushFollow(FOLLOW_log_in_mult1156);
            	    o3=log();

            	    state._fsp--;
            	    if (state.failed) return o;
            	    if ( state.backtracking==0 ) {
            	       o = new Expr.Div(o, o3); 
            	    }

            	    }
            	    break;

            	default :
            	    break loop10;
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
        return o;
    }
    // $ANTLR end "mult"


    // $ANTLR start "log"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:113:1: log returns [Operand o] : (o1= exp | 'log10' '(' o3= exp ')' | 'log' '(' o2= exp ')' );
    public final Operand log() throws RecognitionException {
        Operand o = null;

        Operand o1 = null;

        Operand o3 = null;

        Operand o2 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:114:5: (o1= exp | 'log10' '(' o3= exp ')' | 'log' '(' o2= exp ')' )
            int alt11=3;
            switch ( input.LA(1) ) {
            case ID:
            case STRING:
            case INT:
            case REAL:
            case 15:
            case 32:
            case 38:
            case 39:
            case 40:
            case 41:
                {
                alt11=1;
                }
                break;
            case 35:
                {
                alt11=2;
                }
                break;
            case 36:
                {
                alt11=3;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return o;}
                NoViableAltException nvae =
                    new NoViableAltException("", 11, 0, input);

                throw nvae;
            }

            switch (alt11) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:115:5: o1= exp
                    {
                    pushFollow(FOLLOW_exp_in_log1214);
                    o1=exp();

                    state._fsp--;
                    if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = o1; 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:116:4: 'log10' '(' o3= exp ')'
                    {
                    match(input,35,FOLLOW_35_in_log1247); if (state.failed) return o;
                    match(input,15,FOLLOW_15_in_log1249); if (state.failed) return o;
                    pushFollow(FOLLOW_exp_in_log1253);
                    o3=exp();

                    state._fsp--;
                    if (state.failed) return o;
                    match(input,16,FOLLOW_16_in_log1255); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Expr.Log10(o3); 
                    }

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:117:4: 'log' '(' o2= exp ')'
                    {
                    match(input,36,FOLLOW_36_in_log1270); if (state.failed) return o;
                    match(input,15,FOLLOW_15_in_log1272); if (state.failed) return o;
                    pushFollow(FOLLOW_exp_in_log1276);
                    o2=exp();

                    state._fsp--;
                    if (state.failed) return o;
                    match(input,16,FOLLOW_16_in_log1278); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Expr.Log(o2); 
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
    // $ANTLR end "log"


    // $ANTLR start "exp"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:120:1: exp returns [Operand o] : o1= fun ( '^' o2= fun )? ;
    public final Operand exp() throws RecognitionException {
        Operand o = null;

        Operand o1 = null;

        Operand o2 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:121:5: (o1= fun ( '^' o2= fun )? )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:122:2: o1= fun ( '^' o2= fun )?
            {
            pushFollow(FOLLOW_fun_in_exp1314);
            o1=fun();

            state._fsp--;
            if (state.failed) return o;
            if ( state.backtracking==0 ) {
               o = o1; 
            }
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:123:2: ( '^' o2= fun )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==37) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:123:3: '^' o2= fun
                    {
                    match(input,37,FOLLOW_37_in_exp1346); if (state.failed) return o;
                    pushFollow(FOLLOW_fun_in_exp1350);
                    o2=fun();

                    state._fsp--;
                    if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Expr.Pow(o, o2); 
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
        return o;
    }
    // $ANTLR end "exp"


    // $ANTLR start "fun"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:127:1: fun returns [Operand o] : ( atom | ( ID '(' (o1= expr ( ',' o2= expr )* )? ')' ) );
    public final Operand fun() throws RecognitionException {
        Operand o = null;

        Token ID9=null;
        Operand o1 = null;

        Operand o2 = null;

        Operand atom8 = null;



            	ArrayList<Operand> args = new ArrayList<Operand>();
            	String id = "?";
            
        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:132:5: ( atom | ( ID '(' (o1= expr ( ',' o2= expr )* )? ')' ) )
            int alt15=2;
            int LA15_0 = input.LA(1);

            if ( ((LA15_0>=STRING && LA15_0<=REAL)||LA15_0==15||LA15_0==32||(LA15_0>=38 && LA15_0<=41)) ) {
                alt15=1;
            }
            else if ( (LA15_0==ID) ) {
                int LA15_2 = input.LA(2);

                if ( (LA15_2==15) ) {
                    alt15=2;
                }
                else if ( (LA15_2==EOF||(LA15_2>=13 && LA15_2<=14)||(LA15_2>=16 && LA15_2<=34)||LA15_2==37) ) {
                    alt15=1;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return o;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 15, 2, input);

                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return o;}
                NoViableAltException nvae =
                    new NoViableAltException("", 15, 0, input);

                throw nvae;
            }
            switch (alt15) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:133:2: atom
                    {
                    pushFollow(FOLLOW_atom_in_fun1408);
                    atom8=atom();

                    state._fsp--;
                    if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = atom8; 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:134:4: ( ID '(' (o1= expr ( ',' o2= expr )* )? ')' )
                    {
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:134:4: ( ID '(' (o1= expr ( ',' o2= expr )* )? ')' )
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:134:6: ID '(' (o1= expr ( ',' o2= expr )* )? ')'
                    {
                    ID9=(Token)match(input,ID,FOLLOW_ID_in_fun1446); if (state.failed) return o;
                    match(input,15,FOLLOW_15_in_fun1448); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       id=(ID9!=null?ID9.getText():null); 
                    }
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:135:6: (o1= expr ( ',' o2= expr )* )?
                    int alt14=2;
                    int LA14_0 = input.LA(1);

                    if ( ((LA14_0>=ID && LA14_0<=REAL)||LA14_0==15||LA14_0==32||(LA14_0>=35 && LA14_0<=36)||(LA14_0>=38 && LA14_0<=41)) ) {
                        alt14=1;
                    }
                    switch (alt14) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:135:7: o1= expr ( ',' o2= expr )*
                            {
                            pushFollow(FOLLOW_expr_in_fun1482);
                            o1=expr();

                            state._fsp--;
                            if (state.failed) return o;
                            if ( state.backtracking==0 ) {
                               args = new ArrayList<Operand>(); args.add(o1); 
                            }
                            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:136:7: ( ',' o2= expr )*
                            loop13:
                            do {
                                int alt13=2;
                                int LA13_0 = input.LA(1);

                                if ( (LA13_0==24) ) {
                                    alt13=1;
                                }


                                switch (alt13) {
                            	case 1 :
                            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:136:8: ',' o2= expr
                            	    {
                            	    match(input,24,FOLLOW_24_in_fun1513); if (state.failed) return o;
                            	    pushFollow(FOLLOW_expr_in_fun1517);
                            	    o2=expr();

                            	    state._fsp--;
                            	    if (state.failed) return o;
                            	    if ( state.backtracking==0 ) {
                            	       args.add(o2); 
                            	    }

                            	    }
                            	    break;

                            	default :
                            	    break loop13;
                                }
                            } while (true);


                            }
                            break;

                    }

                    match(input,16,FOLLOW_16_in_fun1558); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Expr.Func(id,args); 
                    }

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
    // $ANTLR end "fun"


    // $ANTLR start "atom"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:143:1: atom returns [Operand o] : ( 'null' | 'return' | 'EPS' | numOrVar | STRING | 'old' '(' e1= expr ')' | '(' e2= expr ')' );
    public final Operand atom() throws RecognitionException {
        Operand o = null;

        Token STRING11=null;
        Operand e1 = null;

        Operand e2 = null;

        Operand numOrVar10 = null;


        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:143:25: ( 'null' | 'return' | 'EPS' | numOrVar | STRING | 'old' '(' e1= expr ')' | '(' e2= expr ')' )
            int alt16=7;
            switch ( input.LA(1) ) {
            case 38:
                {
                alt16=1;
                }
                break;
            case 39:
                {
                alt16=2;
                }
                break;
            case 40:
                {
                alt16=3;
                }
                break;
            case ID:
            case INT:
            case REAL:
            case 32:
                {
                alt16=4;
                }
                break;
            case STRING:
                {
                alt16=5;
                }
                break;
            case 41:
                {
                alt16=6;
                }
                break;
            case 15:
                {
                alt16=7;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return o;}
                NoViableAltException nvae =
                    new NoViableAltException("", 16, 0, input);

                throw nvae;
            }

            switch (alt16) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:144:5: 'null'
                    {
                    match(input,38,FOLLOW_38_in_atom1607); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = Operand.NULL; 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:145:7: 'return'
                    {
                    match(input,39,FOLLOW_39_in_atom1643); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Expr.Result(); 
                    }

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:146:7: 'EPS'
                    {
                    match(input,40,FOLLOW_40_in_atom1675); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = Operand.EPS; 
                    }

                    }
                    break;
                case 4 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:147:4: numOrVar
                    {
                    pushFollow(FOLLOW_numOrVar_in_atom1707);
                    numOrVar10=numOrVar();

                    state._fsp--;
                    if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = numOrVar10; 
                    }

                    }
                    break;
                case 5 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:148:4: STRING
                    {
                    STRING11=(Token)match(input,STRING,FOLLOW_STRING_in_atom1736); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Operand.Const((STRING11!=null?STRING11.getText():null)); 
                    }

                    }
                    break;
                case 6 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:149:4: 'old' '(' e1= expr ')'
                    {
                    match(input,41,FOLLOW_41_in_atom1767); if (state.failed) return o;
                    match(input,15,FOLLOW_15_in_atom1769); if (state.failed) return o;
                    pushFollow(FOLLOW_expr_in_atom1773);
                    e1=expr();

                    state._fsp--;
                    if (state.failed) return o;
                    match(input,16,FOLLOW_16_in_atom1775); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Expr.Old(e1); 
                    }

                    }
                    break;
                case 7 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:150:4: '(' e2= expr ')'
                    {
                    match(input,15,FOLLOW_15_in_atom1791); if (state.failed) return o;
                    pushFollow(FOLLOW_expr_in_atom1795);
                    e2=expr();

                    state._fsp--;
                    if (state.failed) return o;
                    match(input,16,FOLLOW_16_in_atom1797); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = e2; 
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
    // $ANTLR end "atom"


    // $ANTLR start "numOrVar"
    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:153:1: numOrVar returns [Operand o] : ( '-' )? (i2= ID | INT | REAL ) ;
    public final Operand numOrVar() throws RecognitionException {
        Operand o = null;

        Token i2=null;
        Token INT12=null;
        Token REAL13=null;

        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:153:29: ( ( '-' )? (i2= ID | INT | REAL ) )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:154:5: ( '-' )? (i2= ID | INT | REAL )
            {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:154:5: ( '-' )?
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( (LA17_0==32) ) {
                alt17=1;
            }
            switch (alt17) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:0:0: '-'
                    {
                    match(input,32,FOLLOW_32_in_numOrVar1831); if (state.failed) return o;

                    }
                    break;

            }

            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:155:2: (i2= ID | INT | REAL )
            int alt18=3;
            switch ( input.LA(1) ) {
            case ID:
                {
                alt18=1;
                }
                break;
            case INT:
                {
                alt18=2;
                }
                break;
            case REAL:
                {
                alt18=3;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return o;}
                NoViableAltException nvae =
                    new NoViableAltException("", 18, 0, input);

                throw nvae;
            }

            switch (alt18) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:155:3: i2= ID
                    {
                    i2=(Token)match(input,ID,FOLLOW_ID_in_numOrVar1838); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Operand.VarRef((i2!=null?i2.getText():null)); 
                    }

                    }
                    break;
                case 2 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:156:4: INT
                    {
                    INT12=(Token)match(input,INT,FOLLOW_INT_in_numOrVar1870); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Operand.Const(new Integer((INT12!=null?INT12.getText():null))); 
                    }

                    }
                    break;
                case 3 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:157:4: REAL
                    {
                    REAL13=(Token)match(input,REAL,FOLLOW_REAL_in_numOrVar1904); if (state.failed) return o;
                    if ( state.backtracking==0 ) {
                       o = new Operand.Const(new Double((REAL13!=null?REAL13.getText():null))); 
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
        return o;
    }
    // $ANTLR end "numOrVar"

    // $ANTLR start synpred3_ContractSpec
    public final void synpred3_ContractSpec_fragment() throws RecognitionException {   
        // /Users/pcmehlitz/projects/grammars/ContractSpec.g:51:2: ( simpleContract )
        // /Users/pcmehlitz/projects/grammars/ContractSpec.g:51:2: simpleContract
        {
        pushFollow(FOLLOW_simpleContract_in_synpred3_ContractSpec264);
        simpleContract();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred3_ContractSpec

    // Delegated rules

    public final boolean synpred3_ContractSpec() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred3_ContractSpec_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }


    protected DFA3 dfa3 = new DFA3(this);
    static final String DFA3_eotS =
        "\16\uffff";
    static final String DFA3_eofS =
        "\16\uffff";
    static final String DFA3_minS =
        "\1\4\11\uffff\1\0\3\uffff";
    static final String DFA3_maxS =
        "\1\51\11\uffff\1\0\3\uffff";
    static final String DFA3_acceptS =
        "\1\uffff\1\1\13\uffff\1\2";
    static final String DFA3_specialS =
        "\12\uffff\1\0\3\uffff}>";
    static final String[] DFA3_transitionS = {
            "\4\1\7\uffff\1\12\20\uffff\1\1\2\uffff\2\1\1\uffff\4\1",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\uffff",
            "",
            "",
            ""
    };

    static final short[] DFA3_eot = DFA.unpackEncodedString(DFA3_eotS);
    static final short[] DFA3_eof = DFA.unpackEncodedString(DFA3_eofS);
    static final char[] DFA3_min = DFA.unpackEncodedStringToUnsignedChars(DFA3_minS);
    static final char[] DFA3_max = DFA.unpackEncodedStringToUnsignedChars(DFA3_maxS);
    static final short[] DFA3_accept = DFA.unpackEncodedString(DFA3_acceptS);
    static final short[] DFA3_special = DFA.unpackEncodedString(DFA3_specialS);
    static final short[][] DFA3_transition;

    static {
        int numStates = DFA3_transitionS.length;
        DFA3_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA3_transition[i] = DFA.unpackEncodedString(DFA3_transitionS[i]);
        }
    }

    class DFA3 extends DFA {

        public DFA3(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 3;
            this.eot = DFA3_eot;
            this.eof = DFA3_eof;
            this.min = DFA3_min;
            this.max = DFA3_max;
            this.accept = DFA3_accept;
            this.special = DFA3_special;
            this.transition = DFA3_transition;
        }
        public String getDescription() {
            return "49:1: contractAtom returns [Contract c] : ( simpleContract | '(' contract ')' );";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA3_10 = input.LA(1);

                         
                        int index3_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred3_ContractSpec()) ) {s = 1;}

                        else if ( (true) ) {s = 13;}

                         
                        input.seek(index3_10);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 3, _s, input);
            error(nvae);
            throw nvae;
        }
    }
 

    public static final BitSet FOLLOW_contract_in_contractSpec64 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_contractAnd_in_contract114 = new BitSet(new long[]{0x0000000000002002L});
    public static final BitSet FOLLOW_13_in_contract141 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_contractAnd_in_contract145 = new BitSet(new long[]{0x0000000000002002L});
    public static final BitSet FOLLOW_contractAtom_in_contractAnd194 = new BitSet(new long[]{0x0000000000004002L});
    public static final BitSet FOLLOW_14_in_contractAnd220 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_contractAtom_in_contractAnd224 = new BitSet(new long[]{0x0000000000004002L});
    public static final BitSet FOLLOW_simpleContract_in_contractAtom264 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_contractAtom289 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_contract_in_contractAtom291 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_contractAtom293 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expr_in_simpleContract328 = new BitSet(new long[]{0x000000007CFE0000L});
    public static final BitSet FOLLOW_17_in_simpleContract334 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract338 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_18_in_simpleContract366 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract370 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_19_in_simpleContract395 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract399 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_20_in_simpleContract425 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract429 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_21_in_simpleContract454 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract458 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_simpleContract484 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract488 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_simpleContract513 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract517 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_24_in_simpleContract527 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract531 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_simpleContract557 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_simpleContract561 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_simpleContract588 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_simpleContract619 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_simpleContract649 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ID_in_simpleContract651 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_simpleContract676 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_STRING_in_simpleContract678 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_genericContract_in_simpleContract702 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_genericContract754 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ID_in_genericContract756 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_15_in_genericContract781 = new BitSet(new long[]{0x000003D9000180F0L});
    public static final BitSet FOLLOW_expr_in_genericContract820 = new BitSet(new long[]{0x0000000001010000L});
    public static final BitSet FOLLOW_24_in_genericContract851 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_genericContract855 = new BitSet(new long[]{0x0000000001010000L});
    public static final BitSet FOLLOW_16_in_genericContract897 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mult_in_expr955 = new BitSet(new long[]{0x0000000180000002L});
    public static final BitSet FOLLOW_31_in_expr990 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_mult_in_expr994 = new BitSet(new long[]{0x0000000180000002L});
    public static final BitSet FOLLOW_32_in_expr1023 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_mult_in_expr1027 = new BitSet(new long[]{0x0000000180000002L});
    public static final BitSet FOLLOW_log_in_mult1085 = new BitSet(new long[]{0x0000000600000002L});
    public static final BitSet FOLLOW_33_in_mult1118 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_log_in_mult1122 = new BitSet(new long[]{0x0000000600000002L});
    public static final BitSet FOLLOW_34_in_mult1152 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_log_in_mult1156 = new BitSet(new long[]{0x0000000600000002L});
    public static final BitSet FOLLOW_exp_in_log1214 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_log1247 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_15_in_log1249 = new BitSet(new long[]{0x000003C1000080F0L});
    public static final BitSet FOLLOW_exp_in_log1253 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_log1255 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_log1270 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_15_in_log1272 = new BitSet(new long[]{0x000003C1000080F0L});
    public static final BitSet FOLLOW_exp_in_log1276 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_log1278 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_fun_in_exp1314 = new BitSet(new long[]{0x0000002000000002L});
    public static final BitSet FOLLOW_37_in_exp1346 = new BitSet(new long[]{0x000003C1000080F0L});
    public static final BitSet FOLLOW_fun_in_exp1350 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atom_in_fun1408 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_fun1446 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_15_in_fun1448 = new BitSet(new long[]{0x000003D9000180F0L});
    public static final BitSet FOLLOW_expr_in_fun1482 = new BitSet(new long[]{0x0000000001010000L});
    public static final BitSet FOLLOW_24_in_fun1513 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_fun1517 = new BitSet(new long[]{0x0000000001010000L});
    public static final BitSet FOLLOW_16_in_fun1558 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_atom1607 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_atom1643 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_atom1675 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_numOrVar_in_atom1707 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_atom1736 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_atom1767 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_15_in_atom1769 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_atom1773 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_atom1775 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_atom1791 = new BitSet(new long[]{0x000003D9000080F0L});
    public static final BitSet FOLLOW_expr_in_atom1795 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_atom1797 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_numOrVar1831 = new BitSet(new long[]{0x00000000000000D0L});
    public static final BitSet FOLLOW_ID_in_numOrVar1838 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_numOrVar1870 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REAL_in_numOrVar1904 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_simpleContract_in_synpred3_ContractSpec264 = new BitSet(new long[]{0x0000000000000002L});

}