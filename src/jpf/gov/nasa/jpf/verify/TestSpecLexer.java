// $ANTLR 3.1.2 /Users/pcmehlitz/projects/grammars/TestSpec.g 2009-05-05 15:46:40

	package gov.nasa.jpf.verify;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class TestSpecLexer extends Lexer {
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
    public static final int T__30=30;
    public static final int T__19=19;
    public static final int REAL=6;
    public static final int STRING_PATTERN=9;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int T__33=33;
    public static final int WS=16;
    public static final int T__34=34;
    public static final int T__18=18;
    public static final int T__35=35;
    public static final int ESCAPE=15;
    public static final int T__17=17;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int NUM_PATTERN=14;
    public static final int INT_PATTERN=5;
    public static final int REAL_PATTERN=7;
    public static final int STRING=8;

    // delegates
    // delegators

    public TestSpecLexer() {;} 
    public TestSpecLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public TestSpecLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/pcmehlitz/projects/grammars/TestSpec.g"; }

    // $ANTLR start "T__17"
    public final void mT__17() throws RecognitionException {
        try {
            int _type = T__17;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:11:7: ( '|' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:11:9: '|'
            {
            match('|'); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:12:7: ( '.' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:12:9: '.'
            {
            match('.'); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:13:7: ( ',' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:13:9: ','
            {
            match(','); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:14:7: ( 'this' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:14:9: 'this'
            {
            match("this"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:15:7: ( '(' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:15:9: '('
            {
            match('('); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:16:7: ( ')' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:16:9: ')'
            {
            match(')'); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:17:7: ( 'true' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:17:9: 'true'
            {
            match("true"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:18:7: ( 'false' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:18:9: 'false'
            {
            match("false"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:19:7: ( 'new' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:19:9: 'new'
            {
            match("new"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:20:7: ( '{' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:20:9: '{'
            {
            match('{'); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:21:7: ( '}' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:21:9: '}'
            {
            match('}'); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:22:7: ( '==' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:22:9: '=='
            {
            match("=="); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:23:7: ( '!=' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:23:9: '!='
            {
            match("!="); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:24:7: ( '<' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:24:9: '<'
            {
            match('<'); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:25:7: ( '<=' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:25:9: '<='
            {
            match("<="); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:26:7: ( '>' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:26:9: '>'
            {
            match('>'); 

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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:27:7: ( '>=' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:27:9: '>='
            {
            match(">="); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:28:7: ( 'matches' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:28:9: 'matches'
            {
            match("matches"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:29:7: ( 'within' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:29:9: 'within'
            {
            match("within"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:30:7: ( '+-' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:30:9: '+-'
            {
            match("+-"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:31:7: ( 'throws' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:31:9: 'throws'
            {
            match("throws"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:32:7: ( 'noThrows' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:32:9: 'noThrows'
            {
            match("noThrows"); 


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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:33:7: ( 'satisfies' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:33:9: 'satisfies'
            {
            match("satisfies"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__39"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:288:4: ( ( 'a' .. 'z' | 'A' .. 'Z' | '$' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:288:7: ( 'a' .. 'z' | 'A' .. 'Z' | '$' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' )*
            {
            if ( input.LA(1)=='$'||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // /Users/pcmehlitz/projects/grammars/TestSpec.g:288:35: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0=='$'||LA1_0=='.'||(LA1_0>='0' && LA1_0<='9')||(LA1_0>='A' && LA1_0<='Z')||LA1_0=='_'||(LA1_0>='a' && LA1_0<='z')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:
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

    // $ANTLR start "ID_PATTERN"
    public final void mID_PATTERN() throws RecognitionException {
        try {
            int _type = ID_PATTERN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:291:3: ( ( 'a' .. 'z' | 'A' .. 'Z' | '$' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' | '*' )* )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:291:5: ( 'a' .. 'z' | 'A' .. 'Z' | '$' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' | '*' )*
            {
            if ( input.LA(1)=='$'||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // /Users/pcmehlitz/projects/grammars/TestSpec.g:291:33: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '$' | '_' | '.' | '*' )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0=='$'||LA2_0=='*'||LA2_0=='.'||(LA2_0>='0' && LA2_0<='9')||(LA2_0>='A' && LA2_0<='Z')||LA2_0=='_'||(LA2_0>='a' && LA2_0<='z')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:
            	    {
            	    if ( input.LA(1)=='$'||input.LA(1)=='*'||input.LA(1)=='.'||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ID_PATTERN"

    // $ANTLR start "SIGN"
    public final void mSIGN() throws RecognitionException {
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:294:5: ( ( '-' | '+' ) )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:294:7: ( '-' | '+' )
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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:297:5: ( ( '0' .. '9' )+ )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:297:7: ( '0' .. '9' )+
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:297:7: ( '0' .. '9' )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='0' && LA3_0<='9')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:297:8: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "NUM"

    // $ANTLR start "NUM_PATTERN"
    public final void mNUM_PATTERN() throws RecognitionException {
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:301:3: ( ( '0' .. '9' | '[' | ']' )+ )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:301:5: ( '0' .. '9' | '[' | ']' )+
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:301:5: ( '0' .. '9' | '[' | ']' )+
            int cnt4=0;
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( ((LA4_0>='0' && LA4_0<='9')||LA4_0=='['||LA4_0==']') ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||input.LA(1)=='['||input.LA(1)==']' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt4 >= 1 ) break loop4;
                        EarlyExitException eee =
                            new EarlyExitException(4, input);
                        throw eee;
                }
                cnt4++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "NUM_PATTERN"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:303:5: ( ( SIGN )? NUM )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:303:7: ( SIGN )? NUM
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:303:7: ( SIGN )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='+'||LA5_0=='-') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:303:7: SIGN
                    {
                    mSIGN(); 

                    }
                    break;

            }

            mNUM(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "INT_PATTERN"
    public final void mINT_PATTERN() throws RecognitionException {
        try {
            int _type = INT_PATTERN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:306:3: ( ( SIGN )? NUM_PATTERN )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:306:5: ( SIGN )? NUM_PATTERN
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:306:5: ( SIGN )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0=='+'||LA6_0=='-') ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:306:5: SIGN
                    {
                    mSIGN(); 

                    }
                    break;

            }

            mNUM_PATTERN(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT_PATTERN"

    // $ANTLR start "REAL"
    public final void mREAL() throws RecognitionException {
        try {
            int _type = REAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:5: ( ( ( SIGN )? NUM )? '.' NUM ( 'e' ( SIGN )? NUM )? )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:7: ( ( SIGN )? NUM )? '.' NUM ( 'e' ( SIGN )? NUM )?
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:7: ( ( SIGN )? NUM )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0=='+'||LA8_0=='-'||(LA8_0>='0' && LA8_0<='9')) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:8: ( SIGN )? NUM
                    {
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:8: ( SIGN )?
                    int alt7=2;
                    int LA7_0 = input.LA(1);

                    if ( (LA7_0=='+'||LA7_0=='-') ) {
                        alt7=1;
                    }
                    switch (alt7) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:8: SIGN
                            {
                            mSIGN(); 

                            }
                            break;

                    }

                    mNUM(); 

                    }
                    break;

            }

            match('.'); 
            mNUM(); 
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:28: ( 'e' ( SIGN )? NUM )?
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0=='e') ) {
                alt10=1;
            }
            switch (alt10) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:29: 'e' ( SIGN )? NUM
                    {
                    match('e'); 
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:33: ( SIGN )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0=='+'||LA9_0=='-') ) {
                        alt9=1;
                    }
                    switch (alt9) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/TestSpec.g:308:33: SIGN
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

    // $ANTLR start "REAL_PATTERN"
    public final void mREAL_PATTERN() throws RecognitionException {
        try {
            int _type = REAL_PATTERN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:3: ( ( ( SIGN )? NUM_PATTERN )? '.' NUM_PATTERN ( 'e' ( SIGN )? NUM_PATTERN )? )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:5: ( ( SIGN )? NUM_PATTERN )? '.' NUM_PATTERN ( 'e' ( SIGN )? NUM_PATTERN )?
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:5: ( ( SIGN )? NUM_PATTERN )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0=='+'||LA12_0=='-'||(LA12_0>='0' && LA12_0<='9')||LA12_0=='['||LA12_0==']') ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:6: ( SIGN )? NUM_PATTERN
                    {
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:6: ( SIGN )?
                    int alt11=2;
                    int LA11_0 = input.LA(1);

                    if ( (LA11_0=='+'||LA11_0=='-') ) {
                        alt11=1;
                    }
                    switch (alt11) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:6: SIGN
                            {
                            mSIGN(); 

                            }
                            break;

                    }

                    mNUM_PATTERN(); 

                    }
                    break;

            }

            match('.'); 
            mNUM_PATTERN(); 
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:42: ( 'e' ( SIGN )? NUM_PATTERN )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0=='e') ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:43: 'e' ( SIGN )? NUM_PATTERN
                    {
                    match('e'); 
                    // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:47: ( SIGN )?
                    int alt13=2;
                    int LA13_0 = input.LA(1);

                    if ( (LA13_0=='+'||LA13_0=='-') ) {
                        alt13=1;
                    }
                    switch (alt13) {
                        case 1 :
                            // /Users/pcmehlitz/projects/grammars/TestSpec.g:311:47: SIGN
                            {
                            mSIGN(); 

                            }
                            break;

                    }

                    mNUM_PATTERN(); 

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
    // $ANTLR end "REAL_PATTERN"

    // $ANTLR start "ESCAPE"
    public final void mESCAPE() throws RecognitionException {
        try {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:315:3: ( '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' ) )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:315:7: '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' )
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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:323:3: ( '\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\'' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:323:6: '\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\''
            {
            match('\''); 
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:323:11: ( ESCAPE | ~ ( '\\\\' | '\\'' ) )*
            loop15:
            do {
                int alt15=3;
                int LA15_0 = input.LA(1);

                if ( (LA15_0=='\\') ) {
                    alt15=1;
                }
                else if ( ((LA15_0>='\u0000' && LA15_0<='&')||(LA15_0>='(' && LA15_0<='[')||(LA15_0>=']' && LA15_0<='\uFFFF')) ) {
                    alt15=2;
                }


                switch (alt15) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:323:13: ESCAPE
            	    {
            	    mESCAPE(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:323:22: ~ ( '\\\\' | '\\'' )
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
            	    break loop15;
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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:331:3: ( '#\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\'' )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:331:5: '#\\'' ( ESCAPE | ~ ( '\\\\' | '\\'' ) )* '\\''
            {
            match("#'"); 

            // /Users/pcmehlitz/projects/grammars/TestSpec.g:331:11: ( ESCAPE | ~ ( '\\\\' | '\\'' ) )*
            loop16:
            do {
                int alt16=3;
                int LA16_0 = input.LA(1);

                if ( (LA16_0=='\\') ) {
                    alt16=1;
                }
                else if ( ((LA16_0>='\u0000' && LA16_0<='&')||(LA16_0>='(' && LA16_0<='[')||(LA16_0>=']' && LA16_0<='\uFFFF')) ) {
                    alt16=2;
                }


                switch (alt16) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:331:13: ESCAPE
            	    {
            	    mESCAPE(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:331:22: ~ ( '\\\\' | '\\'' )
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
            	    break loop16;
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
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:337:5: ( ( ' ' | '\\t' )+ )
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:337:7: ( ' ' | '\\t' )+
            {
            // /Users/pcmehlitz/projects/grammars/TestSpec.g:337:7: ( ' ' | '\\t' )+
            int cnt17=0;
            loop17:
            do {
                int alt17=2;
                int LA17_0 = input.LA(1);

                if ( (LA17_0=='\t'||LA17_0==' ') ) {
                    alt17=1;
                }


                switch (alt17) {
            	case 1 :
            	    // /Users/pcmehlitz/projects/grammars/TestSpec.g:
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
            	    if ( cnt17 >= 1 ) break loop17;
                        EarlyExitException eee =
                            new EarlyExitException(17, input);
                        throw eee;
                }
                cnt17++;
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
        // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:8: ( T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | ID | ID_PATTERN | INT | INT_PATTERN | REAL | REAL_PATTERN | STRING | STRING_PATTERN | WS )
        int alt18=32;
        alt18 = dfa18.predict(input);
        switch (alt18) {
            case 1 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:10: T__17
                {
                mT__17(); 

                }
                break;
            case 2 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:16: T__18
                {
                mT__18(); 

                }
                break;
            case 3 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:22: T__19
                {
                mT__19(); 

                }
                break;
            case 4 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:28: T__20
                {
                mT__20(); 

                }
                break;
            case 5 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:34: T__21
                {
                mT__21(); 

                }
                break;
            case 6 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:40: T__22
                {
                mT__22(); 

                }
                break;
            case 7 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:46: T__23
                {
                mT__23(); 

                }
                break;
            case 8 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:52: T__24
                {
                mT__24(); 

                }
                break;
            case 9 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:58: T__25
                {
                mT__25(); 

                }
                break;
            case 10 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:64: T__26
                {
                mT__26(); 

                }
                break;
            case 11 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:70: T__27
                {
                mT__27(); 

                }
                break;
            case 12 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:76: T__28
                {
                mT__28(); 

                }
                break;
            case 13 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:82: T__29
                {
                mT__29(); 

                }
                break;
            case 14 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:88: T__30
                {
                mT__30(); 

                }
                break;
            case 15 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:94: T__31
                {
                mT__31(); 

                }
                break;
            case 16 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:100: T__32
                {
                mT__32(); 

                }
                break;
            case 17 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:106: T__33
                {
                mT__33(); 

                }
                break;
            case 18 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:112: T__34
                {
                mT__34(); 

                }
                break;
            case 19 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:118: T__35
                {
                mT__35(); 

                }
                break;
            case 20 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:124: T__36
                {
                mT__36(); 

                }
                break;
            case 21 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:130: T__37
                {
                mT__37(); 

                }
                break;
            case 22 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:136: T__38
                {
                mT__38(); 

                }
                break;
            case 23 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:142: T__39
                {
                mT__39(); 

                }
                break;
            case 24 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:148: ID
                {
                mID(); 

                }
                break;
            case 25 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:151: ID_PATTERN
                {
                mID_PATTERN(); 

                }
                break;
            case 26 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:162: INT
                {
                mINT(); 

                }
                break;
            case 27 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:166: INT_PATTERN
                {
                mINT_PATTERN(); 

                }
                break;
            case 28 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:178: REAL
                {
                mREAL(); 

                }
                break;
            case 29 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:183: REAL_PATTERN
                {
                mREAL_PATTERN(); 

                }
                break;
            case 30 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:196: STRING
                {
                mSTRING(); 

                }
                break;
            case 31 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:203: STRING_PATTERN
                {
                mSTRING_PATTERN(); 

                }
                break;
            case 32 :
                // /Users/pcmehlitz/projects/grammars/TestSpec.g:1:218: WS
                {
                mWS(); 

                }
                break;

        }

    }


    protected DFA18 dfa18 = new DFA18(this);
    static final String DFA18_eotS =
        "\2\uffff\1\32\1\uffff\1\40\2\uffff\2\40\4\uffff\1\46\1\50\2\40\1"+
        "\uffff\2\40\1\uffff\1\55\1\57\4\uffff\1\60\1\uffff\3\40\2\uffff"+
        "\3\40\4\uffff\2\40\1\uffff\1\40\5\uffff\4\40\1\101\4\40\1\uffff"+
        "\1\60\1\106\1\40\1\110\1\40\1\uffff\4\40\1\uffff\1\40\1\uffff\1"+
        "\117\4\40\1\124\1\uffff\2\40\1\127\1\40\1\uffff\1\40\1\132\1\uffff"+
        "\1\40\1\134\1\uffff\1\40\1\uffff\1\136\1\uffff";
    static final String DFA18_eofS =
        "\137\uffff";
    static final String DFA18_minS =
        "\1\11\1\uffff\1\60\1\uffff\1\44\2\uffff\2\44\4\uffff\2\75\2\44\1"+
        "\55\2\44\1\60\2\56\4\uffff\1\60\1\uffff\3\44\2\uffff\3\44\4\uffff"+
        "\2\44\1\uffff\1\44\1\uffff\1\60\2\uffff\1\53\11\44\2\60\4\44\1\uffff"+
        "\4\44\1\uffff\1\44\1\uffff\6\44\1\uffff\4\44\1\uffff\2\44\1\uffff"+
        "\2\44\1\uffff\1\44\1\uffff\1\44\1\uffff";
    static final String DFA18_maxS =
        "\1\175\1\uffff\1\135\1\uffff\1\172\2\uffff\2\172\4\uffff\2\75\2"+
        "\172\1\135\2\172\3\135\4\uffff\1\145\1\uffff\3\172\2\uffff\3\172"+
        "\4\uffff\2\172\1\uffff\1\172\1\uffff\1\135\2\uffff\1\135\11\172"+
        "\2\135\4\172\1\uffff\4\172\1\uffff\1\172\1\uffff\6\172\1\uffff\4"+
        "\172\1\uffff\2\172\1\uffff\2\172\1\uffff\1\172\1\uffff\1\172\1\uffff";
    static final String DFA18_acceptS =
        "\1\uffff\1\1\1\uffff\1\3\1\uffff\1\5\1\6\2\uffff\1\12\1\13\1\14"+
        "\1\15\12\uffff\1\36\1\37\1\40\1\2\1\uffff\1\35\3\uffff\1\30\1\31"+
        "\3\uffff\1\17\1\16\1\21\1\20\2\uffff\1\24\1\uffff\1\32\1\uffff\1"+
        "\33\1\34\20\uffff\1\11\4\uffff\1\4\1\uffff\1\7\6\uffff\1\10\4\uffff"+
        "\1\25\2\uffff\1\23\2\uffff\1\22\1\uffff\1\26\1\uffff\1\27";
    static final String DFA18_specialS =
        "\137\uffff}>";
    static final String[] DFA18_transitionS = {
            "\1\31\26\uffff\1\31\1\14\1\uffff\1\30\1\23\2\uffff\1\27\1\5"+
            "\1\6\1\uffff\1\21\1\3\1\24\1\2\1\uffff\12\25\2\uffff\1\15\1"+
            "\13\1\16\2\uffff\32\23\1\26\1\uffff\1\26\1\uffff\1\23\1\uffff"+
            "\5\23\1\7\6\23\1\17\1\10\4\23\1\22\1\4\2\23\1\20\3\23\1\11\1"+
            "\1\1\12",
            "",
            "\12\33\41\uffff\1\34\1\uffff\1\34",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\7\37\1\35\11\37\1\36\10\37",
            "",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\1\42\31\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\4\37\1\43\11\37\1\44\13\37",
            "",
            "",
            "",
            "",
            "\1\45",
            "\1\47",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\1\51\31\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\10\37\1\52\21\37",
            "\1\53\2\uffff\12\25\41\uffff\1\26\1\uffff\1\26",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\1\54\31\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "\12\25\41\uffff\1\26\1\uffff\1\26",
            "\1\56\1\uffff\12\25\41\uffff\1\26\1\uffff\1\26",
            "\1\34\1\uffff\12\26\41\uffff\1\26\1\uffff\1\26",
            "",
            "",
            "",
            "",
            "\12\33\41\uffff\1\34\1\uffff\1\34\7\uffff\1\61",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\10\37\1\62\10\37\1\63\10\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\24\37\1\64\5\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\13\37\1\65\16\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\26\37\1\66\3\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\23\37"+
            "\1\67\6\37\4\uffff\1\37\1\uffff\32\37",
            "",
            "",
            "",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\23\37\1\70\6\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\23\37\1\71\6\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\23\37\1\72\6\37",
            "",
            "\12\33\41\uffff\1\34\1\uffff\1\34",
            "",
            "",
            "\1\73\1\uffff\1\73\2\uffff\12\74\41\uffff\1\34\1\uffff\1\34",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\22\37\1\75\7\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\16\37\1\76\13\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\4\37\1\77\25\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\22\37\1\100\7\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\7\37\1\102\22\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\2\37\1\103\27\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\7\37\1\104\22\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\10\37\1\105\21\37",
            "\12\74\41\uffff\1\34\1\uffff\1\34",
            "\12\74\41\uffff\1\34\1\uffff\1\34",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\26\37\1\107\3\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\4\37\1\111\25\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\21\37\1\112\10\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\7\37\1\113\22\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\10\37\1\114\21\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\22\37\1\115\7\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\22\37\1\116\7\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\16\37\1\120\13\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\4\37\1\121\25\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\15\37\1\122\14\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\5\37\1\123\24\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\26\37\1\125\3\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\22\37\1\126\7\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\10\37\1\130\21\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\22\37\1\131\7\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\4\37\1\133\25\37",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\22\37\1\135\7\37",
            "",
            "\1\37\5\uffff\1\41\3\uffff\1\37\1\uffff\12\37\7\uffff\32\37"+
            "\4\uffff\1\37\1\uffff\32\37",
            ""
    };

    static final short[] DFA18_eot = DFA.unpackEncodedString(DFA18_eotS);
    static final short[] DFA18_eof = DFA.unpackEncodedString(DFA18_eofS);
    static final char[] DFA18_min = DFA.unpackEncodedStringToUnsignedChars(DFA18_minS);
    static final char[] DFA18_max = DFA.unpackEncodedStringToUnsignedChars(DFA18_maxS);
    static final short[] DFA18_accept = DFA.unpackEncodedString(DFA18_acceptS);
    static final short[] DFA18_special = DFA.unpackEncodedString(DFA18_specialS);
    static final short[][] DFA18_transition;

    static {
        int numStates = DFA18_transitionS.length;
        DFA18_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA18_transition[i] = DFA.unpackEncodedString(DFA18_transitionS[i]);
        }
    }

    class DFA18 extends DFA {

        public DFA18(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 18;
            this.eot = DFA18_eot;
            this.eof = DFA18_eof;
            this.min = DFA18_min;
            this.max = DFA18_max;
            this.accept = DFA18_accept;
            this.special = DFA18_special;
            this.transition = DFA18_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | ID | ID_PATTERN | INT | INT_PATTERN | REAL | REAL_PATTERN | STRING | STRING_PATTERN | WS );";
        }
    }
 

}