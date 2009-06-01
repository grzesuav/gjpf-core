// $ANTLR 3.1.2 /Users/pcmehlitz/projects/grammars/ContractSpec.g 2009-05-05 15:56:37

	package gov.nasa.jpf.verify;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class ContractSpecLexer extends Lexer {
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
    public static final int T__19=19;
    public static final int T__30=30;
    public static final int REAL=7;
    public static final int STRING_PATTERN=11;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int T__16=16;
    public static final int T__33=33;
    public static final int WS=12;
    public static final int T__15=15;
    public static final int T__34=34;
    public static final int T__18=18;
    public static final int T__35=35;
    public static final int ESCAPE=10;
    public static final int T__17=17;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__14=14;
    public static final int T__39=39;
    public static final int T__13=13;
    public static final int STRING=5;

    // delegates
    // delegators

    public ContractSpecLexer() {;} 
    public ContractSpecLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public ContractSpecLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/pcmehlitz/projects/grammars/ContractSpec.g"; }

    // $ANTLR start "T__13"
    public final void mT__13() throws RecognitionException {
        try {
            int _type = T__13;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:11:7: ( '||' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:11:9: '||'
            {
            match("||"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__13"

    // $ANTLR start "T__14"
    public final void mT__14() throws RecognitionException {
        try {
            int _type = T__14;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:12:7: ( '&&' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:12:9: '&&'
            {
            match("&&"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__14"

    // $ANTLR start "T__15"
    public final void mT__15() throws RecognitionException {
        try {
            int _type = T__15;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:13:7: ( '(' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:13:9: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__15"

    // $ANTLR start "T__16"
    public final void mT__16() throws RecognitionException {
        try {
            int _type = T__16;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:14:7: ( ')' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:14:9: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__16"

    // $ANTLR start "T__17"
    public final void mT__17() throws RecognitionException {
        try {
            int _type = T__17;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:15:7: ( '==' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:15:9: '=='
            {
            match("=="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__17"

    // $ANTLR start "T__18"
    public final void mT__18() throws RecognitionException {
        try {
            int _type = T__18;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:16:7: ( '!=' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:16:9: '!='
            {
            match("!="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__18"

    // $ANTLR start "T__19"
    public final void mT__19() throws RecognitionException {
        try {
            int _type = T__19;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:17:7: ( '>' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:17:9: '>'
            {
            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__19"

    // $ANTLR start "T__20"
    public final void mT__20() throws RecognitionException {
        try {
            int _type = T__20;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:18:7: ( '>=' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:18:9: '>='
            {
            match(">="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__20"

    // $ANTLR start "T__21"
    public final void mT__21() throws RecognitionException {
        try {
            int _type = T__21;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:19:7: ( '<' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:19:9: '<'
            {
            match('<'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__21"

    // $ANTLR start "T__22"
    public final void mT__22() throws RecognitionException {
        try {
            int _type = T__22;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:20:7: ( '<=' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:20:9: '<='
            {
            match("<="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__22"

    // $ANTLR start "T__23"
    public final void mT__23() throws RecognitionException {
        try {
            int _type = T__23;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:21:7: ( 'within' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:21:9: 'within'
            {
            match("within"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__23"

    // $ANTLR start "T__24"
    public final void mT__24() throws RecognitionException {
        try {
            int _type = T__24;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:22:7: ( ',' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:22:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__24"

    // $ANTLR start "T__25"
    public final void mT__25() throws RecognitionException {
        try {
            int _type = T__25;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:23:7: ( '+-' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:23:9: '+-'
            {
            match("+-"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__25"

    // $ANTLR start "T__26"
    public final void mT__26() throws RecognitionException {
        try {
            int _type = T__26;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:24:7: ( 'isEmpty' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:24:9: 'isEmpty'
            {
            match("isEmpty"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__26"

    // $ANTLR start "T__27"
    public final void mT__27() throws RecognitionException {
        try {
            int _type = T__27;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:25:7: ( 'notEmpty' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:25:9: 'notEmpty'
            {
            match("notEmpty"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__27"

    // $ANTLR start "T__28"
    public final void mT__28() throws RecognitionException {
        try {
            int _type = T__28;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:26:7: ( 'instanceof' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:26:9: 'instanceof'
            {
            match("instanceof"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__28"

    // $ANTLR start "T__29"
    public final void mT__29() throws RecognitionException {
        try {
            int _type = T__29;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:27:7: ( 'matches' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:27:9: 'matches'
            {
            match("matches"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__29"

    // $ANTLR start "T__30"
    public final void mT__30() throws RecognitionException {
        try {
            int _type = T__30;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:28:7: ( 'satisfies' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:28:9: 'satisfies'
            {
            match("satisfies"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__30"

    // $ANTLR start "T__31"
    public final void mT__31() throws RecognitionException {
        try {
            int _type = T__31;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:29:7: ( '+' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:29:9: '+'
            {
            match('+'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__31"

    // $ANTLR start "T__32"
    public final void mT__32() throws RecognitionException {
        try {
            int _type = T__32;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:30:7: ( '-' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:30:9: '-'
            {
            match('-'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__32"

    // $ANTLR start "T__33"
    public final void mT__33() throws RecognitionException {
        try {
            int _type = T__33;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:31:7: ( '*' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:31:9: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__33"

    // $ANTLR start "T__34"
    public final void mT__34() throws RecognitionException {
        try {
            int _type = T__34;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:32:7: ( '/' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:32:9: '/'
            {
            match('/'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__34"

    // $ANTLR start "T__35"
    public final void mT__35() throws RecognitionException {
        try {
            int _type = T__35;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:33:7: ( 'log10' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:33:9: 'log10'
            {
            match("log10"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__35"

    // $ANTLR start "T__36"
    public final void mT__36() throws RecognitionException {
        try {
            int _type = T__36;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:34:7: ( 'log' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:34:9: 'log'
            {
            match("log"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__36"

    // $ANTLR start "T__37"
    public final void mT__37() throws RecognitionException {
        try {
            int _type = T__37;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:35:7: ( '^' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:35:9: '^'
            {
            match('^'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__37"

    // $ANTLR start "T__38"
    public final void mT__38() throws RecognitionException {
        try {
            int _type = T__38;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:36:7: ( 'null' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:36:9: 'null'
            {
            match("null"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__38"

    // $ANTLR start "T__39"
    public final void mT__39() throws RecognitionException {
        try {
            int _type = T__39;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:37:7: ( 'return' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:37:9: 'return'
            {
            match("return"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__39"

    // $ANTLR start "T__40"
    public final void mT__40() throws RecognitionException {
        try {
            int _type = T__40;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:38:7: ( 'EPS' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:38:9: 'EPS'
            {
            match("EPS"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__40"

    // $ANTLR start "T__41"
    public final void mT__41() throws RecognitionException {
        try {
            int _type = T__41;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:39:7: ( 'old' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:39:9: 'old'
            {
            match("old"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__41"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:164:4: ( ( 'a' .. 'z' | 'A' .. 'Z' | '$' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' )* )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:164:7: ( 'a' .. 'z' | 'A' .. 'Z' | '$' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' )*
            {
            if ( input.LA(1)=='$'||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:164:35: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0=='$'||LA1_0=='.'||(LA1_0>='0' && LA1_0<='9')||(LA1_0>='A' && LA1_0<='Z')||LA1_0=='_'||(LA1_0>='a' && LA1_0<='z')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:
            	    {
            	    if ( input.LA(1)=='$'||input.LA(1)=='.'||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ID"

    // $ANTLR start "SIGN"
    public final void mSIGN() throws RecognitionException {
        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:167:5: ( ( '-' | '+' ) )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:167:7: ( '-' | '+' )
            {
            if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "SIGN"

    // $ANTLR start "NUM"
    public final void mNUM() throws RecognitionException {
        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:170:5: ( ( '0' .. '9' )+ )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:170:7: ( '0' .. '9' )+
            {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:170:7: ( '0' .. '9' )+
            int cnt2=0;
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( ((LA2_0>='0' && LA2_0<='9')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:170:8: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt2 >= 1 ) break loop2;
                        EarlyExitException eee =
                            new EarlyExitException(2, input);
                        throw eee;
                }
                cnt2++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "NUM"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:172:5: ( NUM )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:172:8: NUM
            {
            mNUM(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "REAL"
    public final void mREAL() throws RecognitionException {
        try {
            int _type = REAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:5: ( ( NUM )? '.' NUM ( 'e' ( SIGN )? NUM )? )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:7: ( NUM )? '.' NUM ( 'e' ( SIGN )? NUM )?
            {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:7: ( NUM )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( ((LA3_0>='0' && LA3_0<='9')) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:7: NUM
                    {
                    mNUM(); 

                    }
                    break;

            }

            match('.'); 
            mNUM(); 
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:20: ( 'e' ( SIGN )? NUM )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='e') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:21: 'e' ( SIGN )? NUM
                    {
                    match('e'); 
                    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:25: ( SIGN )?
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0=='+'||LA4_0=='-') ) {
                        alt4=1;
                    }
                    switch (alt4) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:174:25: SIGN
                            {
                            mSIGN(); 

                            }
                            break;

                    }

                    mNUM(); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "REAL"

    // $ANTLR start "ESCAPE"
    public final void mESCAPE() throws RecognitionException {
        try {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:179:3: ( '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' ) )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:179:7: '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' )
            {
            match('\\'); 
            if ( input.LA(1)=='\"'||input.LA(1)=='\''||input.LA(1)=='\\'||input.LA(1)=='b'||input.LA(1)=='f'||input.LA(1)=='n'||input.LA(1)=='r'||input.LA(1)=='t' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "ESCAPE"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:187:3: ( '\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\'' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:187:6: '\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\''
            {
            match('\''); 
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:187:11: ( ESCAPE | ~ ( '\\\\' | '\\'' ) )*
            loop6:
            do {
                int alt6=3;
                int LA6_0 = input.LA(1);

                if ( (LA6_0=='\\') ) {
                    alt6=1;
                }
                else if ( ((LA6_0>='\u0000' && LA6_0<='&')||(LA6_0>='(' && LA6_0<='[')||(LA6_0>=']' && LA6_0<='\uFFFF')) ) {
                    alt6=2;
                }


                switch (alt6) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:187:13: ESCAPE
            	    {
            	    mESCAPE(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:187:22: ~ ( '\\\\' | '\\'' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
              // remove the quotes
                String s = getText();
                setText( s.substring(1, s.length()-1));
                  }
        finally {
        }
    }
    // $ANTLR end "STRING"

    // $ANTLR start "STRING_PATTERN"
    public final void mSTRING_PATTERN() throws RecognitionException {
        try {
            int _type = STRING_PATTERN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:195:3: ( '#\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\'' )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:195:5: '#\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\''
            {
            match("#'"); 

            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:195:11: ( ESCAPE | ~ ( '\\\\' | '\\'' ) )*
            loop7:
            do {
                int alt7=3;
                int LA7_0 = input.LA(1);

                if ( (LA7_0=='\\') ) {
                    alt7=1;
                }
                else if ( ((LA7_0>='\u0000' && LA7_0<='&')||(LA7_0>='(' && LA7_0<='[')||(LA7_0>=']' && LA7_0<='\uFFFF')) ) {
                    alt7=2;
                }


                switch (alt7) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:195:13: ESCAPE
            	    {
            	    mESCAPE(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:195:22: ~ ( '\\\\' | '\\'' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop7;
                }
            } while (true);

            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
              // remove the quotes
                String s = getText();
                setText( s.substring(2, s.length()-1));
                  }
        finally {
        }
    }
    // $ANTLR end "STRING_PATTERN"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:201:5: ( ( ' ' | '\\t' )+ )
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:201:7: ( ' ' | '\\t' )+
            {
            // /Users/pcmehlitz/projects/grammars/ContractSpec.g:201:7: ( ' ' | '\\t' )+
            int cnt8=0;
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( (LA8_0=='\t'||LA8_0==' ') ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/ContractSpec.g:
            	    {
            	    if ( input.LA(1)=='\t'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt8 >= 1 ) break loop8;
                        EarlyExitException eee =
                            new EarlyExitException(8, input);
                        throw eee;
                }
                cnt8++;
            } while (true);

            skip();

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    public void mTokens() throws RecognitionException {
        // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:8: ( T__13 | T__14 | T__15 | T__16 | T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | T__40 | T__41 | ID | INT | REAL | STRING | STRING_PATTERN | WS )
        int alt9=35;
        alt9 = dfa9.predict(input);
        switch (alt9) {
            case 1 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:10: T__13
                {
                mT__13(); 

                }
                break;
            case 2 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:16: T__14
                {
                mT__14(); 

                }
                break;
            case 3 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:22: T__15
                {
                mT__15(); 

                }
                break;
            case 4 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:28: T__16
                {
                mT__16(); 

                }
                break;
            case 5 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:34: T__17
                {
                mT__17(); 

                }
                break;
            case 6 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:40: T__18
                {
                mT__18(); 

                }
                break;
            case 7 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:46: T__19
                {
                mT__19(); 

                }
                break;
            case 8 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:52: T__20
                {
                mT__20(); 

                }
                break;
            case 9 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:58: T__21
                {
                mT__21(); 

                }
                break;
            case 10 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:64: T__22
                {
                mT__22(); 

                }
                break;
            case 11 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:70: T__23
                {
                mT__23(); 

                }
                break;
            case 12 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:76: T__24
                {
                mT__24(); 

                }
                break;
            case 13 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:82: T__25
                {
                mT__25(); 

                }
                break;
            case 14 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:88: T__26
                {
                mT__26(); 

                }
                break;
            case 15 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:94: T__27
                {
                mT__27(); 

                }
                break;
            case 16 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:100: T__28
                {
                mT__28(); 

                }
                break;
            case 17 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:106: T__29
                {
                mT__29(); 

                }
                break;
            case 18 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:112: T__30
                {
                mT__30(); 

                }
                break;
            case 19 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:118: T__31
                {
                mT__31(); 

                }
                break;
            case 20 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:124: T__32
                {
                mT__32(); 

                }
                break;
            case 21 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:130: T__33
                {
                mT__33(); 

                }
                break;
            case 22 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:136: T__34
                {
                mT__34(); 

                }
                break;
            case 23 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:142: T__35
                {
                mT__35(); 

                }
                break;
            case 24 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:148: T__36
                {
                mT__36(); 

                }
                break;
            case 25 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:154: T__37
                {
                mT__37(); 

                }
                break;
            case 26 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:160: T__38
                {
                mT__38(); 

                }
                break;
            case 27 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:166: T__39
                {
                mT__39(); 

                }
                break;
            case 28 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:172: T__40
                {
                mT__40(); 

                }
                break;
            case 29 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:178: T__41
                {
                mT__41(); 

                }
                break;
            case 30 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:184: ID
                {
                mID(); 

                }
                break;
            case 31 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:187: INT
                {
                mINT(); 

                }
                break;
            case 32 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:191: REAL
                {
                mREAL(); 

                }
                break;
            case 33 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:196: STRING
                {
                mSTRING(); 

                }
                break;
            case 34 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:203: STRING_PATTERN
                {
                mSTRING_PATTERN(); 

                }
                break;
            case 35 :
                // /Users/pcmehlitz/projects/grammars/ContractSpec.g:1:218: WS
                {
                mWS(); 

                }
                break;

        }

    }


    protected DFA9 dfa9 = new DFA9(this);
    static final String DFA9_eotS =
        "\7\uffff\1\37\1\41\1\30\1\uffff\1\44\4\30\3\uffff\1\30\1\uffff\3"+
        "\30\1\uffff\1\57\10\uffff\1\30\2\uffff\12\30\1\uffff\7\30\1\103"+
        "\1\30\1\105\1\106\4\30\1\113\3\30\1\uffff\1\30\2\uffff\4\30\1\uffff"+
        "\2\30\1\126\1\30\1\130\5\30\1\uffff\1\136\1\uffff\1\137\2\30\1\142"+
        "\1\30\2\uffff\1\30\1\145\1\uffff\2\30\1\uffff\1\150\1\151\2\uffff";
    static final String DFA9_eofS =
        "\152\uffff";
    static final String DFA9_minS =
        "\1\11\6\uffff\2\75\1\151\1\uffff\1\55\1\156\1\157\2\141\3\uffff"+
        "\1\157\1\uffff\1\145\1\120\1\154\1\uffff\1\56\10\uffff\1\164\2\uffff"+
        "\1\105\1\163\1\164\1\154\2\164\1\147\1\164\1\123\1\144\1\uffff\1"+
        "\150\1\155\1\164\1\105\1\154\1\143\1\151\1\44\1\165\2\44\1\151\1"+
        "\160\1\141\1\155\1\44\1\150\1\163\1\60\1\uffff\1\162\2\uffff\1\156"+
        "\1\164\1\156\1\160\1\uffff\1\145\1\146\1\44\1\156\1\44\1\171\1\143"+
        "\1\164\1\163\1\151\1\uffff\1\44\1\uffff\1\44\1\145\1\171\1\44\1"+
        "\145\2\uffff\1\157\1\44\1\uffff\1\163\1\146\1\uffff\2\44\2\uffff";
    static final String DFA9_maxS =
        "\1\174\6\uffff\2\75\1\151\1\uffff\1\55\1\163\1\165\2\141\3\uffff"+
        "\1\157\1\uffff\1\145\1\120\1\154\1\uffff\1\71\10\uffff\1\164\2\uffff"+
        "\1\105\1\163\1\164\1\154\2\164\1\147\1\164\1\123\1\144\1\uffff\1"+
        "\150\1\155\1\164\1\105\1\154\1\143\1\151\1\172\1\165\2\172\1\151"+
        "\1\160\1\141\1\155\1\172\1\150\1\163\1\60\1\uffff\1\162\2\uffff"+
        "\1\156\1\164\1\156\1\160\1\uffff\1\145\1\146\1\172\1\156\1\172\1"+
        "\171\1\143\1\164\1\163\1\151\1\uffff\1\172\1\uffff\1\172\1\145\1"+
        "\171\1\172\1\145\2\uffff\1\157\1\172\1\uffff\1\163\1\146\1\uffff"+
        "\2\172\2\uffff";
    static final String DFA9_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\3\uffff\1\14\5\uffff\1\24\1\25"+
        "\1\26\1\uffff\1\31\3\uffff\1\36\1\uffff\1\40\1\41\1\42\1\43\1\10"+
        "\1\7\1\12\1\11\1\uffff\1\15\1\23\12\uffff\1\37\23\uffff\1\30\1\uffff"+
        "\1\34\1\35\4\uffff\1\32\12\uffff\1\27\1\uffff\1\13\5\uffff\1\33"+
        "\1\16\2\uffff\1\21\2\uffff\1\17\2\uffff\1\22\1\20";
    static final String DFA9_specialS =
        "\152\uffff}>";
    static final String[] DFA9_transitionS = {
            "\1\35\26\uffff\1\35\1\6\1\uffff\1\34\1\30\1\uffff\1\2\1\33\1"+
            "\3\1\4\1\21\1\13\1\12\1\20\1\32\1\22\12\31\2\uffff\1\10\1\5"+
            "\1\7\2\uffff\4\30\1\26\25\30\3\uffff\1\24\1\30\1\uffff\10\30"+
            "\1\14\2\30\1\23\1\16\1\15\1\27\2\30\1\25\1\17\3\30\1\11\3\30"+
            "\1\uffff\1\1",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\36",
            "\1\40",
            "\1\42",
            "",
            "\1\43",
            "\1\46\4\uffff\1\45",
            "\1\47\5\uffff\1\50",
            "\1\51",
            "\1\52",
            "",
            "",
            "",
            "\1\53",
            "",
            "\1\54",
            "\1\55",
            "\1\56",
            "",
            "\1\32\1\uffff\12\31",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\60",
            "",
            "",
            "\1\61",
            "\1\62",
            "\1\63",
            "\1\64",
            "\1\65",
            "\1\66",
            "\1\67",
            "\1\70",
            "\1\71",
            "\1\72",
            "",
            "\1\73",
            "\1\74",
            "\1\75",
            "\1\76",
            "\1\77",
            "\1\100",
            "\1\101",
            "\1\30\11\uffff\1\30\1\uffff\1\30\1\102\10\30\7\uffff\32\30"+
            "\4\uffff\1\30\1\uffff\32\30",
            "\1\104",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\107",
            "\1\110",
            "\1\111",
            "\1\112",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\114",
            "\1\115",
            "\1\116",
            "",
            "\1\117",
            "",
            "",
            "\1\120",
            "\1\121",
            "\1\122",
            "\1\123",
            "",
            "\1\124",
            "\1\125",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\127",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\131",
            "\1\132",
            "\1\133",
            "\1\134",
            "\1\135",
            "",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\140",
            "\1\141",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\143",
            "",
            "",
            "\1\144",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "",
            "\1\146",
            "\1\147",
            "",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "\1\30\11\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1\30"+
            "\1\uffff\32\30",
            "",
            ""
    };

    static final short[] DFA9_eot = DFA.unpackEncodedString(DFA9_eotS);
    static final short[] DFA9_eof = DFA.unpackEncodedString(DFA9_eofS);
    static final char[] DFA9_min = DFA.unpackEncodedStringToUnsignedChars(DFA9_minS);
    static final char[] DFA9_max = DFA.unpackEncodedStringToUnsignedChars(DFA9_maxS);
    static final short[] DFA9_accept = DFA.unpackEncodedString(DFA9_acceptS);
    static final short[] DFA9_special = DFA.unpackEncodedString(DFA9_specialS);
    static final short[][] DFA9_transition;

    static {
        int numStates = DFA9_transitionS.length;
        DFA9_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA9_transition[i] = DFA.unpackEncodedString(DFA9_transitionS[i]);
        }
    }

    class DFA9 extends DFA {

        public DFA9(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 9;
            this.eot = DFA9_eot;
            this.eof = DFA9_eof;
            this.min = DFA9_min;
            this.max = DFA9_max;
            this.accept = DFA9_accept;
            this.special = DFA9_special;
            this.transition = DFA9_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__13 | T__14 | T__15 | T__16 | T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | T__40 | T__41 | ID | INT | REAL | STRING | STRING_PATTERN | WS );";
        }
    }
 

}