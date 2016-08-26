// $ANTLR 3.5.2 src/Query.g 2016-08-26 00:19:54
package com.ruesga.rview.gerrit.filter.antlr;


import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.DFA;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;

@SuppressWarnings("all")
public class QueryLexer extends Lexer {
	public static final int EOF=-1;
	public static final int T__13=13;
	public static final int T__14=14;
	public static final int T__15=15;
	public static final int T__16=16;
	public static final int AND=4;
	public static final int DEFAULT_FIELD=5;
	public static final int EXACT_PHRASE=6;
	public static final int FIELD_NAME=7;
	public static final int NON_WORD=8;
	public static final int NOT=9;
	public static final int OR=10;
	public static final int SINGLE_WORD=11;
	public static final int WS=12;

	// delegates
	// delegators
	public Lexer[] getDelegates() {
		return new Lexer[] {};
	}

	public QueryLexer() {}
	public QueryLexer(CharStream input) {
		this(input, new RecognizerSharedState());
	}
	public QueryLexer(CharStream input, RecognizerSharedState state) {
		super(input,state);
	}

	// $ANTLR start "T__13"
	public final void mT__13() throws RecognitionException {
		try {
			int _type = T__13;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:20:7: ( '(' )
			// src/Query.g:20:9: '('
			{
				match('(');
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__13"

	// $ANTLR start "T__14"
	public final void mT__14() throws RecognitionException {
		try {
			int _type = T__14;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:21:7: ( ')' )
			// src/Query.g:21:9: ')'
			{
				match(')');
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__14"

	// $ANTLR start "T__15"
	public final void mT__15() throws RecognitionException {
		try {
			int _type = T__15;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:22:7: ( '-' )
			// src/Query.g:22:9: '-'
			{
				match('-');
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__15"

	// $ANTLR start "T__16"
	public final void mT__16() throws RecognitionException {
		try {
			int _type = T__16;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:23:7: ( ':' )
			// src/Query.g:23:9: ':'
			{
				match(':');
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__16"

	// $ANTLR start "AND"
	public final void mAND() throws RecognitionException {
		try {
			int _type = AND;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:133:4: ( 'AND' )
			// src/Query.g:133:6: 'AND'
			{
				match("AND");

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "AND"

	// $ANTLR start "OR"
	public final void mOR() throws RecognitionException {
		try {
			int _type = OR;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:134:3: ( 'OR' )
			// src/Query.g:134:6: 'OR'
			{
				match("OR");

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "OR"

	// $ANTLR start "NOT"
	public final void mNOT() throws RecognitionException {
		try {
			int _type = NOT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:135:4: ( 'NOT' )
			// src/Query.g:135:6: 'NOT'
			{
				match("NOT");

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NOT"

	// $ANTLR start "WS"
	public final void mWS() throws RecognitionException {
		try {
			int _type = WS;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:138:3: ( ( ' ' | '\\r' | '\\t' | '\\n' ) )
			// src/Query.g:138:6: ( ' ' | '\\r' | '\\t' | '\\n' )
			{
				if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')
                        ||input.LA(1)=='\r'||input.LA(1)==' ' ) {
					input.consume();
				}
				else {
					MismatchedSetException mse = new MismatchedSetException(null,input);
					recover(mse);
					throw mse;
				}
				_channel=HIDDEN;
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "WS"

	// $ANTLR start "FIELD_NAME"
	public final void mFIELD_NAME() throws RecognitionException {
		try {
			int _type = FIELD_NAME;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:142:3: ( ( 'a' .. 'z' | '_' )+ )
			// src/Query.g:142:5: ( 'a' .. 'z' | '_' )+
			{
				// src/Query.g:142:5: ( 'a' .. 'z' | '_' )+
				int cnt1=0;
				loop1:
				while (true) {
					int alt1=2;
					int LA1_0 = input.LA(1);
					if ( (LA1_0=='_'||(LA1_0 >= 'a' && LA1_0 <= 'z')) ) {
						alt1=1;
					}

					switch (alt1) {
						case 1 :
							// src/Query.g:
						{
							if ( input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
								input.consume();
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								recover(mse);
								throw mse;
							}
						}
						break;

						default :
							if ( cnt1 >= 1 ) break loop1;
							EarlyExitException eee = new EarlyExitException(1, input);
							throw eee;
					}
					cnt1++;
				}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "FIELD_NAME"

	// $ANTLR start "EXACT_PHRASE"
	public final void mEXACT_PHRASE() throws RecognitionException {
		try {
			int _type = EXACT_PHRASE;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:146:3: ( '\"' (~ ( '\"' ) )* '\"' | '{' (~ ( '{' | '}' ) )* '}' )
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0=='\"') ) {
				alt4=1;
			}
			else if ( (LA4_0=='{') ) {
				alt4=2;
			}

			else {
				NoViableAltException nvae =
						new NoViableAltException("", 4, 0, input);
				throw nvae;
			}

			switch (alt4) {
				case 1 :
					// src/Query.g:146:5: '\"' (~ ( '\"' ) )* '\"'
				{
					match('\"');
					// src/Query.g:146:9: (~ ( '\"' ) )*
					loop2:
					while (true) {
						int alt2=2;
						int LA2_0 = input.LA(1);
						if ( ((LA2_0 >= '\u0000' && LA2_0 <= '!')||(LA2_0 >= '#'
                                && LA2_0 <= '\uFFFF')) ) {
							alt2=1;
						}

						switch (alt2) {
							case 1 :
								// src/Query.g:
							{
								if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '!')
                                        ||(input.LA(1) >= '#' && input.LA(1) <= '\uFFFF') ) {
									input.consume();
								}
								else {
									MismatchedSetException mse =
                                            new MismatchedSetException(null,input);
									recover(mse);
									throw mse;
								}
							}
							break;

							default :
								break loop2;
						}
					}

					match('\"');
					String s = getText();
					setText(s.substring(1, s.length() - 1));

				}
				break;
				case 2 :
					// src/Query.g:150:5: '{' (~ ( '{' | '}' ) )* '}'
				{
					match('{');
					// src/Query.g:150:9: (~ ( '{' | '}' ) )*
					loop3:
					while (true) {
						int alt3=2;
						int LA3_0 = input.LA(1);
						if ( ((LA3_0 >= '\u0000' && LA3_0 <= 'z')||LA3_0=='|'
                                ||(LA3_0 >= '~' && LA3_0 <= '\uFFFF')) ) {
							alt3=1;
						}

						switch (alt3) {
							case 1 :
								// src/Query.g:
							{
								if ( (input.LA(1) >= '\u0000' && input.LA(1) <= 'z')
                                        ||input.LA(1)=='|'||(input.LA(1) >= '~'
                                        && input.LA(1) <= '\uFFFF') ) {
									input.consume();
								}
								else {
									MismatchedSetException mse =
                                            new MismatchedSetException(null,input);
									recover(mse);
									throw mse;
								}
							}
							break;

							default :
								break loop3;
						}
					}

					match('}');

					String s = getText();
					setText(s.substring(1, s.length() - 1));

				}
				break;

			}
			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "EXACT_PHRASE"

	// $ANTLR start "SINGLE_WORD"
	public final void mSINGLE_WORD() throws RecognitionException {
		try {
			int _type = SINGLE_WORD;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// src/Query.g:157:3: (~ ( '-' | NON_WORD ) (~ ( NON_WORD ) )* )
			// src/Query.g:157:5: ~ ( '-' | NON_WORD ) (~ ( NON_WORD ) )*
			{
				if ( input.LA(1)=='#'||(input.LA(1) >= '*' && input.LA(1) <= ',')
                        ||(input.LA(1) >= '.' && input.LA(1) <= '9')||(input.LA(1) >= '<'
                        && input.LA(1) <= '>')||(input.LA(1) >= '@' && input.LA(1) <= 'Z')
                        ||input.LA(1)=='\\'||(input.LA(1) >= '^' && input.LA(1) <= 'z')
                        ||input.LA(1)=='|'||(input.LA(1) >= '~' && input.LA(1) <= '\uFFFF') ) {
					input.consume();
				}
				else {
					MismatchedSetException mse = new MismatchedSetException(null,input);
					recover(mse);
					throw mse;
				}
				// src/Query.g:157:25: (~ ( NON_WORD ) )*
				loop5:
				while (true) {
					int alt5=2;
					int LA5_0 = input.LA(1);
					if ( (LA5_0=='#'||(LA5_0 >= '*' && LA5_0 <= '9')||(LA5_0 >= '<' && LA5_0 <= '>')
                            ||(LA5_0 >= '@' && LA5_0 <= 'Z')||LA5_0=='\\'||(LA5_0 >= '^'
                            && LA5_0 <= 'z')||LA5_0=='|'||(LA5_0 >= '~' && LA5_0 <= '\uFFFF')) ) {
						alt5=1;
					}

					switch (alt5) {
						case 1 :
							// src/Query.g:
						{
							if ( input.LA(1)=='#'||(input.LA(1) >= '*' && input.LA(1) <= '9')
                                    ||(input.LA(1) >= '<' && input.LA(1) <= '>')
                                    ||(input.LA(1) >= '@' && input.LA(1) <= 'Z')||input.LA(1)=='\\'
                                    ||(input.LA(1) >= '^' && input.LA(1) <= 'z')||input.LA(1)=='|'
                                    ||(input.LA(1) >= '~' && input.LA(1) <= '\uFFFF') ) {
								input.consume();
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								recover(mse);
								throw mse;
							}
						}
						break;

						default :
							break loop5;
					}
				}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "SINGLE_WORD"

	// $ANTLR start "NON_WORD"
	public final void mNON_WORD() throws RecognitionException {
		try {
			// src/Query.g:160:3: ( ( '\\u0000' .. ' ' | '!' | '\"' | '$' | '%' | '&' | '\\''
            //      | '(' | ')' | ':' | ';' | '?' | '[' | ']' | '{' | '}' ) )
			// src/Query.g:
			{
				if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '\"')||(input.LA(1) >= '$'
						&& input.LA(1) <= ')')||(input.LA(1) >= ':' && input.LA(1) <= ';')
						||input.LA(1)=='?'||input.LA(1)=='['||input.LA(1)==']'
						||input.LA(1)=='{'||input.LA(1)=='}' ) {
					input.consume();
				}
				else {
					MismatchedSetException mse = new MismatchedSetException(null,input);
					recover(mse);
					throw mse;
				}
			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NON_WORD"

	@Override
	public void mTokens() throws RecognitionException {
		// src/Query.g:1:8: ( T__13 | T__14 | T__15 | T__16 | AND | OR | NOT | WS
        //      | FIELD_NAME | EXACT_PHRASE | SINGLE_WORD )
		int alt6=11;
		alt6 = dfa6.predict(input);
		switch (alt6) {
			case 1 :
				// src/Query.g:1:10: T__13
			{
				mT__13();

			}
			break;
			case 2 :
				// src/Query.g:1:16: T__14
			{
				mT__14();

			}
			break;
			case 3 :
				// src/Query.g:1:22: T__15
			{
				mT__15();

			}
			break;
			case 4 :
				// src/Query.g:1:28: T__16
			{
				mT__16();

			}
			break;
			case 5 :
				// src/Query.g:1:34: AND
			{
				mAND();

			}
			break;
			case 6 :
				// src/Query.g:1:38: OR
			{
				mOR();

			}
			break;
			case 7 :
				// src/Query.g:1:41: NOT
			{
				mNOT();

			}
			break;
			case 8 :
				// src/Query.g:1:45: WS
			{
				mWS();

			}
			break;
			case 9 :
				// src/Query.g:1:48: FIELD_NAME
			{
				mFIELD_NAME();

			}
			break;
			case 10 :
				// src/Query.g:1:59: EXACT_PHRASE
			{
				mEXACT_PHRASE();

			}
			break;
			case 11 :
				// src/Query.g:1:72: SINGLE_WORD
			{
				mSINGLE_WORD();

			}
			break;

		}
	}


	protected DFA6 dfa6 = new DFA6(this);
	static final String DFA6_eotS =
			"\5\uffff\3\13\1\uffff\1\17\2\uffff\1\13\1\22\1\13\1\uffff\1\17\1\24\1"+
					"\uffff\1\25\2\uffff";
	static final String DFA6_eofS =
			"\26\uffff";
	static final String DFA6_minS =
			"\1\11\4\uffff\1\116\1\122\1\117\1\uffff\1\43\2\uffff\1\104\1\43\1\124"+
					"\1\uffff\2\43\1\uffff\1\43\2\uffff";
	static final String DFA6_maxS =
			"\1\uffff\4\uffff\1\116\1\122\1\117\1\uffff\1\uffff\2\uffff\1\104\1\uffff"+
					"\1\124\1\uffff\2\uffff\1\uffff\1\uffff\2\uffff";
	static final String DFA6_acceptS =
			"\1\uffff\1\1\1\2\1\3\1\4\3\uffff\1\10\1\uffff\1\12\1\13\3\uffff\1\11\2"+
					"\uffff\1\6\1\uffff\1\5\1\7";
	static final String DFA6_specialS =
			"\26\uffff}>";
	static final String[] DFA6_transitionS = {
			"\2\10\2\uffff\1\10\22\uffff\1\10\1\uffff\1\12\1\13\4\uffff\1\1\1\2\3"+
					"\13\1\3\14\13\1\4\1\uffff\3\13\1\uffff\1\13\1\5\14\13\1\7\1\6\13\13\1"+
					"\uffff\1\13\1\uffff\1\13\1\11\1\13\32\11\1\12\1\13\1\uffff\uff82\13",
			"",
			"",
			"",
			"",
			"\1\14",
			"\1\15",
			"\1\16",
			"",
			"\1\13\6\uffff\20\13\2\uffff\3\13\1\uffff\33\13\1\uffff\1\13\1\uffff"+
					"\1\13\1\20\1\13\32\20\1\uffff\1\13\1\uffff\uff82\13",
			"",
			"",
			"\1\21",
			"\1\13\6\uffff\20\13\2\uffff\3\13\1\uffff\33\13\1\uffff\1\13\1\uffff"+
					"\35\13\1\uffff\1\13\1\uffff\uff82\13",
			"\1\23",
			"",
			"\1\13\6\uffff\20\13\2\uffff\3\13\1\uffff\33\13\1\uffff\1\13\1\uffff"+
					"\1\13\1\20\1\13\32\20\1\uffff\1\13\1\uffff\uff82\13",
			"\1\13\6\uffff\20\13\2\uffff\3\13\1\uffff\33\13\1\uffff\1\13\1\uffff"+
					"\35\13\1\uffff\1\13\1\uffff\uff82\13",
			"",
			"\1\13\6\uffff\20\13\2\uffff\3\13\1\uffff\33\13\1\uffff\1\13\1\uffff"+
					"\35\13\1\uffff\1\13\1\uffff\uff82\13",
			"",
			""
	};

	static final short[] DFA6_eot = DFA.unpackEncodedString(DFA6_eotS);
	static final short[] DFA6_eof = DFA.unpackEncodedString(DFA6_eofS);
	static final char[] DFA6_min = DFA.unpackEncodedStringToUnsignedChars(DFA6_minS);
	static final char[] DFA6_max = DFA.unpackEncodedStringToUnsignedChars(DFA6_maxS);
	static final short[] DFA6_accept = DFA.unpackEncodedString(DFA6_acceptS);
	static final short[] DFA6_special = DFA.unpackEncodedString(DFA6_specialS);
	static final short[][] DFA6_transition;

	static {
		int numStates = DFA6_transitionS.length;
		DFA6_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA6_transition[i] = DFA.unpackEncodedString(DFA6_transitionS[i]);
		}
	}

	protected class DFA6 extends DFA {

		public DFA6(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 6;
			this.eot = DFA6_eot;
			this.eof = DFA6_eof;
			this.min = DFA6_min;
			this.max = DFA6_max;
			this.accept = DFA6_accept;
			this.special = DFA6_special;
			this.transition = DFA6_transition;
		}
		@Override
		public String getDescription() {
			return "1:1: Tokens : ( T__13 | T__14 | T__15 | T__16 | AND " +
					"| OR | NOT | WS | FIELD_NAME | EXACT_PHRASE | SINGLE_WORD );";
		}
	}

}
