// $ANTLR 3.5.2 src/Query.g 2016-08-26 00:19:54
package com.ruesga.rview.gerrit.filter.antlr;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.RuleReturnScope;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.RewriteEarlyExitException;
import org.antlr.runtime.tree.RewriteRuleSubtreeStream;
import org.antlr.runtime.tree.RewriteRuleTokenStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeAdaptor;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("all")
public class QueryParser extends Parser {
    public static final String[] tokenNames = new String[] {
            "<invalid>", "<EOR>", "<DOWN>", "<UP>", "AND", "DEFAULT_FIELD", "EXACT_PHRASE",
            "FIELD_NAME", "NON_WORD", "NOT", "OR", "SINGLE_WORD", "WS", "'('", "')'",
            "'-'", "':'"
    };
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
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

    // delegators
    public QueryParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }
    public QueryParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    static class QueryParseInternalException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        QueryParseInternalException(final String msg) {
            super(msg);
        }
    }

    public static Tree parse(final String str)
            throws QueryParseException {
        try {
            final QueryParser p = new QueryParser(
                    new TokenRewriteStream(
                            new QueryLexer(
                                    new ANTLRStringStream(str)
                            )
                    )
            );
            return (Tree)p.query().getTree();
        } catch (QueryParseInternalException e) {
            throw new QueryParseException(e.getMessage());
        } catch (RecognitionException e) {
            throw new QueryParseException(e.getMessage());
        }
    }

    public static class query_return extends ParserRuleReturnScope {
        Object tree;
        @Override
        public Object getTree() { return tree; }
    };

    // $ANTLR start "query"
    // src/Query.g:92:1: query : conditionOr ;
    public final query_return query() throws RecognitionException {
        query_return retval = new query_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        ParserRuleReturnScope conditionOr1 =null;


        try {
            // src/Query.g:93:3: ( conditionOr )
            // src/Query.g:93:5: conditionOr
            {
                root_0 = (Object)adaptor.nil();
                pushFollow(FOLLOW_conditionOr_in_query101);
                conditionOr1=conditionOr();
                state._fsp--;
                if (state.failed) return retval;
                if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionOr1.getTree());

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
            retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
        }
        finally {
            // do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "query"


    public static class conditionOr_return extends ParserRuleReturnScope {
        Object tree;
        @Override
        public Object getTree() { return tree; }
    };


    // $ANTLR start "conditionOr"
    // src/Query.g:96:1: conditionOr : ( ( conditionAnd OR )=> conditionAnd
    //      OR ^ conditionAnd ( OR ! conditionAnd )* | conditionAnd );
    public final conditionOr_return conditionOr() throws RecognitionException {
        conditionOr_return retval = new conditionOr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token OR3=null;
        Token OR5=null;
        ParserRuleReturnScope conditionAnd2 =null;
        ParserRuleReturnScope conditionAnd4 =null;
        ParserRuleReturnScope conditionAnd6 =null;
        ParserRuleReturnScope conditionAnd7 =null;

        Object OR3_tree=null;
        Object OR5_tree=null;

        try {
            // src/Query.g:97:3: ( ( conditionAnd OR )=> conditionAnd OR ^ conditionAnd
            //      ( OR ! conditionAnd )* | conditionAnd )
            int alt2=2;
            switch ( input.LA(1) ) {
                case 15:
                {
                    int LA2_1 = input.LA(2);
                    if ( (synpred1_Query()) ) {
                        alt2=1;
                    }
                    else if ( (true) ) {
                        alt2=2;
                    }

                }
                break;
                case NOT:
                {
                    int LA2_2 = input.LA(2);
                    if ( (synpred1_Query()) ) {
                        alt2=1;
                    }
                    else if ( (true) ) {
                        alt2=2;
                    }

                }
                break;
                case 13:
                {
                    int LA2_3 = input.LA(2);
                    if ( (synpred1_Query()) ) {
                        alt2=1;
                    }
                    else if ( (true) ) {
                        alt2=2;
                    }

                }
                break;
                case FIELD_NAME:
                {
                    int LA2_4 = input.LA(2);
                    if ( (synpred1_Query()) ) {
                        alt2=1;
                    }
                    else if ( (true) ) {
                        alt2=2;
                    }

                }
                break;
                case SINGLE_WORD:
                {
                    int LA2_5 = input.LA(2);
                    if ( (synpred1_Query()) ) {
                        alt2=1;
                    }
                    else if ( (true) ) {
                        alt2=2;
                    }

                }
                break;
                case EXACT_PHRASE:
                {
                    int LA2_6 = input.LA(2);
                    if ( (synpred1_Query()) ) {
                        alt2=1;
                    }
                    else if ( (true) ) {
                        alt2=2;
                    }

                }
                break;
                default:
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                            new NoViableAltException("", 2, 0, input);
                    throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // src/Query.g:97:5: ( conditionAnd OR )=> conditionAnd
                    //      OR ^ conditionAnd ( OR ! conditionAnd )*
                {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_conditionAnd_in_conditionOr126);
                    conditionAnd2=conditionAnd();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionAnd2.getTree());

                    OR3=(Token)match(input,OR,FOLLOW_OR_in_conditionOr128);
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                        OR3_tree = (Object)adaptor.create(OR3);
                        root_0 = (Object)adaptor.becomeRoot(OR3_tree, root_0);
                    }

                    pushFollow(FOLLOW_conditionAnd_in_conditionOr131);
                    conditionAnd4=conditionAnd();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionAnd4.getTree());

                    // src/Query.g:98:38: ( OR ! conditionAnd )*
                    loop1:
                    while (true) {
                        int alt1=2;
                        int LA1_0 = input.LA(1);
                        if ( (LA1_0==OR) ) {
                            alt1=1;
                        }

                        switch (alt1) {
                            case 1 :
                                // src/Query.g:98:39: OR ! conditionAnd
                            {
                                OR5=(Token)match(input,OR,FOLLOW_OR_in_conditionOr134);
                                if (state.failed) return retval;
                                pushFollow(FOLLOW_conditionAnd_in_conditionOr137);
                                conditionAnd6=conditionAnd();
                                state._fsp--;
                                if (state.failed) return retval;
                                if ( state.backtracking==0 )
                                    adaptor.addChild(root_0, conditionAnd6.getTree());

                            }
                            break;

                            default :
                                break loop1;
                        }
                    }

                }
                break;
                case 2 :
                    // src/Query.g:99:5: conditionAnd
                {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_conditionAnd_in_conditionOr145);
                    conditionAnd7=conditionAnd();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionAnd7.getTree());

                }
                break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
            retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
        }
        finally {
            // do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "conditionOr"


    public static class conditionAnd_return extends ParserRuleReturnScope {
        Object tree;
        @Override
        public Object getTree() { return tree; }
    };


    // $ANTLR start "conditionAnd"
    // src/Query.g:102:1: conditionAnd : ( ( conditionNot AND )=>i+= conditionNot
    //      (i+= conditionAnd2 )* -> ^( AND ( $i)+ ) |
    //      ( conditionNot conditionNot )=>i+= conditionNot (i+= conditionAnd2 )*
    //      -> ^( AND ( $i)+ ) | conditionNot );
    public final conditionAnd_return conditionAnd() throws RecognitionException {
        conditionAnd_return retval = new conditionAnd_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        List<Object> list_i=null;
        ParserRuleReturnScope conditionNot8 =null;
        RuleReturnScope i = null;
        RewriteRuleSubtreeStream stream_conditionAnd2 =
                new RewriteRuleSubtreeStream(adaptor,"rule conditionAnd2");
        RewriteRuleSubtreeStream stream_conditionNot =
                new RewriteRuleSubtreeStream(adaptor,"rule conditionNot");

        try {
            // src/Query.g:103:3: ( ( conditionNot AND )=>i+= conditionNot (i+= conditionAnd2 )*
            //      -> ^( AND ( $i)+ ) | ( conditionNot conditionNot )=>i+=
            //      conditionNot (i+= conditionAnd2 )* -> ^( AND ( $i)+ ) | conditionNot )
            int alt5=3;
            switch ( input.LA(1) ) {
                case 15:
                {
                    int LA5_1 = input.LA(2);
                    if ( (synpred2_Query()) ) {
                        alt5=1;
                    }
                    else if ( (synpred3_Query()) ) {
                        alt5=2;
                    }
                    else if ( (true) ) {
                        alt5=3;
                    }

                }
                break;
                case NOT:
                {
                    int LA5_2 = input.LA(2);
                    if ( (synpred2_Query()) ) {
                        alt5=1;
                    }
                    else if ( (synpred3_Query()) ) {
                        alt5=2;
                    }
                    else if ( (true) ) {
                        alt5=3;
                    }

                }
                break;
                case 13:
                {
                    int LA5_3 = input.LA(2);
                    if ( (synpred2_Query()) ) {
                        alt5=1;
                    }
                    else if ( (synpred3_Query()) ) {
                        alt5=2;
                    }
                    else if ( (true) ) {
                        alt5=3;
                    }

                }
                break;
                case FIELD_NAME:
                {
                    int LA5_4 = input.LA(2);
                    if ( (synpred2_Query()) ) {
                        alt5=1;
                    }
                    else if ( (synpred3_Query()) ) {
                        alt5=2;
                    }
                    else if ( (true) ) {
                        alt5=3;
                    }

                }
                break;
                case SINGLE_WORD:
                {
                    int LA5_5 = input.LA(2);
                    if ( (synpred2_Query()) ) {
                        alt5=1;
                    }
                    else if ( (synpred3_Query()) ) {
                        alt5=2;
                    }
                    else if ( (true) ) {
                        alt5=3;
                    }

                }
                break;
                case EXACT_PHRASE:
                {
                    int LA5_6 = input.LA(2);
                    if ( (synpred2_Query()) ) {
                        alt5=1;
                    }
                    else if ( (synpred3_Query()) ) {
                        alt5=2;
                    }
                    else if ( (true) ) {
                        alt5=3;
                    }

                }
                break;
                default:
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                            new NoViableAltException("", 5, 0, input);
                    throw nvae;
            }
            switch (alt5) {
                case 1 :
                    // src/Query.g:103:5: ( conditionNot AND )=>i+= conditionNot (i+= conditionAnd2 )*
                {
                    pushFollow(FOLLOW_conditionNot_in_conditionAnd172);
                    i=conditionNot();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) stream_conditionNot.add(i.getTree());
                    if (list_i==null) list_i=new ArrayList<Object>();
                    list_i.add(i.getTree());
                    // src/Query.g:104:24: (i+= conditionAnd2 )*
                    loop3:
                    while (true) {
                        int alt3=2;
                        int LA3_0 = input.LA(1);
                        if ( (LA3_0==AND||(LA3_0 >= EXACT_PHRASE && LA3_0 <= FIELD_NAME)
                                ||LA3_0==NOT||LA3_0==SINGLE_WORD||LA3_0==13||LA3_0==15) ) {
                            alt3=1;
                        }

                        switch (alt3) {
                            case 1 :
                                // src/Query.g:104:25: i+= conditionAnd2
                            {
                                pushFollow(FOLLOW_conditionAnd2_in_conditionAnd177);
                                i=conditionAnd2();
                                state._fsp--;
                                if (state.failed) return retval;
                                if ( state.backtracking==0 ) stream_conditionAnd2.add(i.getTree());
                                if (list_i==null) list_i=new ArrayList<Object>();
                                list_i.add(i.getTree());
                            }
                            break;

                            default :
                                break loop3;
                        }
                    }

                    // AST REWRITE
                    // elements: i
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels: i
                    // wildcard labels:
                    if ( state.backtracking==0 ) {
                        retval.tree = root_0;
                        RewriteRuleSubtreeStream stream_retval=
                                new RewriteRuleSubtreeStream(adaptor,"rule retval",
                                        retval!=null?retval.getTree():null);
                        RewriteRuleSubtreeStream stream_i=
                                new RewriteRuleSubtreeStream(adaptor,"token i",list_i);
                        root_0 = (Object)adaptor.nil();
                        // 105:5: -> ^( AND ( $i)+ )
                        {
                            // src/Query.g:105:8: ^( AND ( $i)+ )
                            {
                                Object root_1 = (Object)adaptor.nil();
                                root_1 = (Object)adaptor.becomeRoot(
                                        (Object)adaptor.create(AND, "AND"), root_1);
                                if ( !(stream_i.hasNext()) ) {
                                    throw new RewriteEarlyExitException();
                                }
                                while ( stream_i.hasNext() ) {
                                    adaptor.addChild(root_1, stream_i.nextTree());
                                }
                                stream_i.reset();

                                adaptor.addChild(root_0, root_1);
                            }

                        }


                        retval.tree = root_0;
                    }

                }
                break;
                case 2 :
                    // src/Query.g:106:5: ( conditionNot conditionNot )=>i+=
                    //      conditionNot (i+= conditionAnd2 )*
                {
                    pushFollow(FOLLOW_conditionNot_in_conditionAnd213);
                    i=conditionNot();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) stream_conditionNot.add(i.getTree());
                    if (list_i==null) list_i=new ArrayList<Object>();
                    list_i.add(i.getTree());
                    // src/Query.g:107:24: (i+= conditionAnd2 )*
                    loop4:
                    while (true) {
                        int alt4=2;
                        int LA4_0 = input.LA(1);
                        if ( (LA4_0==AND||(LA4_0 >= EXACT_PHRASE && LA4_0 <= FIELD_NAME)
                                ||LA4_0==NOT||LA4_0==SINGLE_WORD||LA4_0==13||LA4_0==15) ) {
                            alt4=1;
                        }

                        switch (alt4) {
                            case 1 :
                                // src/Query.g:107:25: i+= conditionAnd2
                            {
                                pushFollow(FOLLOW_conditionAnd2_in_conditionAnd218);
                                i=conditionAnd2();
                                state._fsp--;
                                if (state.failed) return retval;
                                if ( state.backtracking==0 ) stream_conditionAnd2.add(i.getTree());
                                if (list_i==null) list_i=new ArrayList<Object>();
                                list_i.add(i.getTree());
                            }
                            break;

                            default :
                                break loop4;
                        }
                    }

                    // AST REWRITE
                    // elements: i
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels: i
                    // wildcard labels:
                    if ( state.backtracking==0 ) {
                        retval.tree = root_0;
                        RewriteRuleSubtreeStream stream_retval =
                                new RewriteRuleSubtreeStream(adaptor,"rule retval",
                                        retval!=null?retval.getTree():null);
                        RewriteRuleSubtreeStream stream_i =
                                new RewriteRuleSubtreeStream(adaptor,"token i",list_i);
                        root_0 = (Object)adaptor.nil();
                        // 108:5: -> ^( AND ( $i)+ )
                        {
                            // src/Query.g:108:8: ^( AND ( $i)+ )
                            {
                                Object root_1 = (Object)adaptor.nil();
                                root_1 = (Object)adaptor.becomeRoot(
                                        (Object)adaptor.create(AND, "AND"), root_1);
                                if ( !(stream_i.hasNext()) ) {
                                    throw new RewriteEarlyExitException();
                                }
                                while ( stream_i.hasNext() ) {
                                    adaptor.addChild(root_1, stream_i.nextTree());
                                }
                                stream_i.reset();

                                adaptor.addChild(root_0, root_1);
                            }

                        }


                        retval.tree = root_0;
                    }

                }
                break;
                case 3 :
                    // src/Query.g:109:5: conditionNot
                {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_conditionNot_in_conditionAnd240);
                    conditionNot8=conditionNot();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionNot8.getTree());

                }
                break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
            retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
        }
        finally {
            // do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "conditionAnd"


    public static class conditionAnd2_return extends ParserRuleReturnScope {
        Object tree;
        @Override
        public Object getTree() { return tree; }
    };


    // $ANTLR start "conditionAnd2"
    // src/Query.g:111:1: conditionAnd2 : ( AND ! conditionNot | conditionNot );
    public final conditionAnd2_return conditionAnd2() throws RecognitionException {
        conditionAnd2_return retval = new conditionAnd2_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token AND9=null;
        ParserRuleReturnScope conditionNot10 =null;
        ParserRuleReturnScope conditionNot11 =null;

        Object AND9_tree=null;

        try {
            // src/Query.g:112:3: ( AND ! conditionNot | conditionNot )
            int alt6=2;
            int LA6_0 = input.LA(1);
            if ( (LA6_0==AND) ) {
                alt6=1;
            }
            else if ( ((LA6_0 >= EXACT_PHRASE && LA6_0 <= FIELD_NAME)||LA6_0==NOT
                    ||LA6_0==SINGLE_WORD||LA6_0==13||LA6_0==15) ) {
                alt6=2;
            }

            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                        new NoViableAltException("", 6, 0, input);
                throw nvae;
            }

            switch (alt6) {
                case 1 :
                    // src/Query.g:112:5: AND ! conditionNot
                {
                    root_0 = (Object)adaptor.nil();


                    AND9=(Token)match(input,AND,FOLLOW_AND_in_conditionAnd2252);
                    if (state.failed) return retval;
                    pushFollow(FOLLOW_conditionNot_in_conditionAnd2255);
                    conditionNot10=conditionNot();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionNot10.getTree());

                }
                break;
                case 2 :
                    // src/Query.g:113:5: conditionNot
                {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_conditionNot_in_conditionAnd2261);
                    conditionNot11=conditionNot();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionNot11.getTree());

                }
                break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
            retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
        }
        finally {
            // do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "conditionAnd2"


    public static class conditionNot_return extends ParserRuleReturnScope {
        Object tree;
        @Override
        public Object getTree() { return tree; }
    };


    // $ANTLR start "conditionNot"
    // src/Query.g:116:1: conditionNot : ( '-' conditionBase -> ^( NOT conditionBase )
    //      | NOT ^ conditionBase | conditionBase );
    public final conditionNot_return conditionNot() throws RecognitionException {
        conditionNot_return retval = new conditionNot_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal12=null;
        Token NOT14=null;
        ParserRuleReturnScope conditionBase13 =null;
        ParserRuleReturnScope conditionBase15 =null;
        ParserRuleReturnScope conditionBase16 =null;

        Object char_literal12_tree=null;
        Object NOT14_tree=null;
        RewriteRuleTokenStream stream_15=new RewriteRuleTokenStream(adaptor,"token 15");
        RewriteRuleSubtreeStream stream_conditionBase =
                new RewriteRuleSubtreeStream(adaptor,"rule conditionBase");

        try {
            // src/Query.g:117:3: ( '-' conditionBase -> ^( NOT conditionBase ) |
            //      NOT ^ conditionBase | conditionBase )
            int alt7=3;
            switch ( input.LA(1) ) {
                case 15:
                {
                    alt7=1;
                }
                break;
                case NOT:
                {
                    alt7=2;
                }
                break;
                case EXACT_PHRASE:
                case FIELD_NAME:
                case SINGLE_WORD:
                case 13:
                {
                    alt7=3;
                }
                break;
                default:
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                            new NoViableAltException("", 7, 0, input);
                    throw nvae;
            }
            switch (alt7) {
                case 1 :
                    // src/Query.g:117:5: '-' conditionBase
                {
                    char_literal12=(Token)match(input,15,FOLLOW_15_in_conditionNot274);
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) stream_15.add(char_literal12);

                    pushFollow(FOLLOW_conditionBase_in_conditionNot276);
                    conditionBase13=conditionBase();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) stream_conditionBase.add(conditionBase13.getTree());
                    // AST REWRITE
                    // elements: conditionBase
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels:
                    // wildcard labels:
                    if ( state.backtracking==0 ) {
                        retval.tree = root_0;
                        RewriteRuleSubtreeStream stream_retval =
                                new RewriteRuleSubtreeStream(adaptor,"rule retval",
                                        retval!=null?retval.getTree():null);

                        root_0 = (Object)adaptor.nil();
                        // 117:23: -> ^( NOT conditionBase )
                        {
                            // src/Query.g:117:26: ^( NOT conditionBase )
                            {
                                Object root_1 = (Object)adaptor.nil();
                                root_1 = (Object)adaptor.becomeRoot(
                                        (Object)adaptor.create(NOT, "NOT"), root_1);
                                adaptor.addChild(root_1, stream_conditionBase.nextTree());
                                adaptor.addChild(root_0, root_1);
                            }

                        }


                        retval.tree = root_0;
                    }

                }
                break;
                case 2 :
                    // src/Query.g:118:5: NOT ^ conditionBase
                {
                    root_0 = (Object)adaptor.nil();


                    NOT14=(Token)match(input,NOT,FOLLOW_NOT_in_conditionNot290);
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                        NOT14_tree = (Object)adaptor.create(NOT14);
                        root_0 = (Object)adaptor.becomeRoot(NOT14_tree, root_0);
                    }

                    pushFollow(FOLLOW_conditionBase_in_conditionNot293);
                    conditionBase15=conditionBase();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionBase15.getTree());

                }
                break;
                case 3 :
                    // src/Query.g:119:5: conditionBase
                {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_conditionBase_in_conditionNot299);
                    conditionBase16=conditionBase();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionBase16.getTree());

                }
                break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
            retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
        }
        finally {
            // do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "conditionNot"


    public static class conditionBase_return extends ParserRuleReturnScope {
        Object tree;
        @Override
        public Object getTree() { return tree; }
    };


    // $ANTLR start "conditionBase"
    // src/Query.g:121:1: conditionBase : ( '(' ! conditionOr ')' !|
    //      ( FIELD_NAME ':' )=> FIELD_NAME ^ ':' ! fieldValue | fieldValue
    //      -> ^( DEFAULT_FIELD fieldValue ) );
    public final conditionBase_return conditionBase() throws RecognitionException {
        conditionBase_return retval = new conditionBase_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal17=null;
        Token char_literal19=null;
        Token FIELD_NAME20=null;
        Token char_literal21=null;
        ParserRuleReturnScope conditionOr18 =null;
        ParserRuleReturnScope fieldValue22 =null;
        ParserRuleReturnScope fieldValue23 =null;

        Object char_literal17_tree=null;
        Object char_literal19_tree=null;
        Object FIELD_NAME20_tree=null;
        Object char_literal21_tree=null;
        RewriteRuleSubtreeStream stream_fieldValue =
                new RewriteRuleSubtreeStream(adaptor,"rule fieldValue");

        try {
            // src/Query.g:122:3: ( '(' ! conditionOr ')' !| ( FIELD_NAME ':' )=> FIELD_NAME ^ ':'
            //      ! fieldValue | fieldValue -> ^( DEFAULT_FIELD fieldValue ) )
            int alt8=3;
            switch ( input.LA(1) ) {
                case 13:
                {
                    alt8=1;
                }
                break;
                case FIELD_NAME:
                {
                    int LA8_2 = input.LA(2);
                    if ( (LA8_2==16) && (synpred4_Query())) {
                        alt8=2;
                    }
                    else if ( (LA8_2==EOF||LA8_2==AND||(LA8_2 >= EXACT_PHRASE
                            && LA8_2 <= FIELD_NAME)||(LA8_2 >= NOT && LA8_2 <= SINGLE_WORD)
                            ||(LA8_2 >= 13 && LA8_2 <= 15)) ) {
                        alt8=3;
                    }

                    else {
                        if (state.backtracking>0) {state.failed=true; return retval;}
                        int nvaeMark = input.mark();
                        try {
                            input.consume();
                            NoViableAltException nvae =
                                    new NoViableAltException("", 8, 2, input);
                            throw nvae;
                        } finally {
                            input.rewind(nvaeMark);
                        }
                    }

                }
                break;
                case EXACT_PHRASE:
                case SINGLE_WORD:
                {
                    alt8=3;
                }
                break;
                default:
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                            new NoViableAltException("", 8, 0, input);
                    throw nvae;
            }
            switch (alt8) {
                case 1 :
                    // src/Query.g:122:5: '(' ! conditionOr ')' !
                {
                    root_0 = (Object)adaptor.nil();


                    char_literal17=(Token)match(input,13,FOLLOW_13_in_conditionBase311);
                    if (state.failed) return retval;
                    pushFollow(FOLLOW_conditionOr_in_conditionBase314);
                    conditionOr18=conditionOr();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, conditionOr18.getTree());

                    char_literal19=(Token)match(input,14,FOLLOW_14_in_conditionBase316);
                    if (state.failed) return retval;
                }
                break;
                case 2 :
                    // src/Query.g:123:5: ( FIELD_NAME ':' )=> FIELD_NAME ^ ':' ! fieldValue
                {
                    root_0 = (Object)adaptor.nil();


                    FIELD_NAME20=(Token)match(input,FIELD_NAME,
                            FOLLOW_FIELD_NAME_in_conditionBase331);
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                        FIELD_NAME20_tree = (Object)adaptor.create(FIELD_NAME20);
                        root_0 = (Object)adaptor.becomeRoot(FIELD_NAME20_tree, root_0);
                    }

                    char_literal21=(Token)match(input,16,FOLLOW_16_in_conditionBase334);
                    if (state.failed) return retval;
                    pushFollow(FOLLOW_fieldValue_in_conditionBase337);
                    fieldValue22=fieldValue();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, fieldValue22.getTree());

                }
                break;
                case 3 :
                    // src/Query.g:124:5: fieldValue
                {
                    pushFollow(FOLLOW_fieldValue_in_conditionBase343);
                    fieldValue23=fieldValue();
                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) stream_fieldValue.add(fieldValue23.getTree());
                    // AST REWRITE
                    // elements: fieldValue
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels:
                    // wildcard labels:
                    if ( state.backtracking==0 ) {
                        retval.tree = root_0;
                        RewriteRuleSubtreeStream stream_retval =
                                new RewriteRuleSubtreeStream(adaptor,"rule retval",
                                        retval!=null?retval.getTree():null);

                        root_0 = (Object)adaptor.nil();
                        // 124:16: -> ^( DEFAULT_FIELD fieldValue )
                        {
                            // src/Query.g:124:19: ^( DEFAULT_FIELD fieldValue )
                            {
                                Object root_1 = (Object)adaptor.nil();
                                root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(
                                        DEFAULT_FIELD, "DEFAULT_FIELD"), root_1);
                                adaptor.addChild(root_1, stream_fieldValue.nextTree());
                                adaptor.addChild(root_0, root_1);
                            }

                        }


                        retval.tree = root_0;
                    }

                }
                break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
            retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
        }
        finally {
            // do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "conditionBase"


    public static class fieldValue_return extends ParserRuleReturnScope {
        Object tree;
        @Override
        public Object getTree() { return tree; }
    };


    // $ANTLR start "fieldValue"
    // src/Query.g:127:1: fieldValue : (n= FIELD_NAME -> SINGLE_WORD[n]
    //      | SINGLE_WORD | EXACT_PHRASE );
    public final fieldValue_return fieldValue() throws RecognitionException {
        fieldValue_return retval = new fieldValue_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token n=null;
        Token SINGLE_WORD24=null;
        Token EXACT_PHRASE25=null;

        Object n_tree=null;
        Object SINGLE_WORD24_tree=null;
        Object EXACT_PHRASE25_tree=null;
        RewriteRuleTokenStream stream_FIELD_NAME =
                new RewriteRuleTokenStream(adaptor,"token FIELD_NAME");

        try {
            // src/Query.g:128:3: (n= FIELD_NAME -> SINGLE_WORD[n] | SINGLE_WORD | EXACT_PHRASE )
            int alt9=3;
            switch ( input.LA(1) ) {
                case FIELD_NAME:
                {
                    alt9=1;
                }
                break;
                case SINGLE_WORD:
                {
                    alt9=2;
                }
                break;
                case EXACT_PHRASE:
                {
                    alt9=3;
                }
                break;
                default:
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                            new NoViableAltException("", 9, 0, input);
                    throw nvae;
            }
            switch (alt9) {
                case 1 :
                    // src/Query.g:128:5: n= FIELD_NAME
                {
                    n=(Token)match(input,FIELD_NAME,FOLLOW_FIELD_NAME_in_fieldValue366);
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) stream_FIELD_NAME.add(n);

                    // AST REWRITE
                    // elements:
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels:
                    // wildcard labels:
                    if ( state.backtracking==0 ) {
                        retval.tree = root_0;
                        RewriteRuleSubtreeStream stream_retval =
                                new RewriteRuleSubtreeStream(adaptor,"rule retval",
                                        retval!=null?retval.getTree():null);

                        root_0 = (Object)adaptor.nil();
                        // 128:20: -> SINGLE_WORD[n]
                        {
                            adaptor.addChild(root_0, (Object)adaptor.create(SINGLE_WORD, n));
                        }


                        retval.tree = root_0;
                    }

                }
                break;
                case 2 :
                    // src/Query.g:129:5: SINGLE_WORD
                {
                    root_0 = (Object)adaptor.nil();


                    SINGLE_WORD24=(Token)match(input,SINGLE_WORD,
                            FOLLOW_SINGLE_WORD_in_fieldValue379);
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                        SINGLE_WORD24_tree = (Object)adaptor.create(SINGLE_WORD24);
                        adaptor.addChild(root_0, SINGLE_WORD24_tree);
                    }

                }
                break;
                case 3 :
                    // src/Query.g:130:5: EXACT_PHRASE
                {
                    root_0 = (Object)adaptor.nil();


                    EXACT_PHRASE25=(Token)match(input,EXACT_PHRASE,
                            FOLLOW_EXACT_PHRASE_in_fieldValue385);
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                        EXACT_PHRASE25_tree = (Object)adaptor.create(EXACT_PHRASE25);
                        adaptor.addChild(root_0, EXACT_PHRASE25_tree);
                    }

                }
                break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
            retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
        }
        finally {
            // do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "fieldValue"

    // $ANTLR start synpred1_Query
    public final void synpred1_Query_fragment() throws RecognitionException {
        // src/Query.g:97:5: ( conditionAnd OR )
        // src/Query.g:97:6: conditionAnd OR
        {
            pushFollow(FOLLOW_conditionAnd_in_synpred1_Query115);
            conditionAnd();
            state._fsp--;
            if (state.failed) return;

            match(input,OR,FOLLOW_OR_in_synpred1_Query117); if (state.failed) return;

        }

    }
    // $ANTLR end synpred1_Query

    // $ANTLR start synpred2_Query
    public final void synpred2_Query_fragment() throws RecognitionException {
        // src/Query.g:103:5: ( conditionNot AND )
        // src/Query.g:103:6: conditionNot AND
        {
            pushFollow(FOLLOW_conditionNot_in_synpred2_Query159);
            conditionNot();
            state._fsp--;
            if (state.failed) return;

            match(input,AND,FOLLOW_AND_in_synpred2_Query161); if (state.failed) return;

        }

    }
    // $ANTLR end synpred2_Query

    // $ANTLR start synpred3_Query
    public final void synpred3_Query_fragment() throws RecognitionException {
        // src/Query.g:106:5: ( conditionNot conditionNot )
        // src/Query.g:106:6: conditionNot conditionNot
        {
            pushFollow(FOLLOW_conditionNot_in_synpred3_Query200);
            conditionNot();
            state._fsp--;
            if (state.failed) return;

            pushFollow(FOLLOW_conditionNot_in_synpred3_Query202);
            conditionNot();
            state._fsp--;
            if (state.failed) return;

        }

    }
    // $ANTLR end synpred3_Query

    // $ANTLR start synpred4_Query
    public final void synpred4_Query_fragment() throws RecognitionException {
        // src/Query.g:123:5: ( FIELD_NAME ':' )
        // src/Query.g:123:6: FIELD_NAME ':'
        {
            match(input,FIELD_NAME,FOLLOW_FIELD_NAME_in_synpred4_Query324);
            if (state.failed) return;

            match(input,16,FOLLOW_16_in_synpred4_Query326); if (state.failed) return;

        }

    }
    // $ANTLR end synpred4_Query

    // Delegated rules

    public final boolean synpred3_Query() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred3_Query_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred1_Query() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred1_Query_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred4_Query() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred4_Query_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred2_Query() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred2_Query_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }



    public static final BitSet FOLLOW_conditionOr_in_query101 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionAnd_in_conditionOr126 =
            new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_OR_in_conditionOr128 =
            new BitSet(new long[]{0x000000000000AAC0L});
    public static final BitSet FOLLOW_conditionAnd_in_conditionOr131 =
            new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_OR_in_conditionOr134 = new BitSet(new long[]{0x000000000000AAC0L});
    public static final BitSet FOLLOW_conditionAnd_in_conditionOr137 =
            new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_conditionAnd_in_conditionOr145 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionNot_in_conditionAnd172 =
            new BitSet(new long[]{0x000000000000AAD2L});
    public static final BitSet FOLLOW_conditionAnd2_in_conditionAnd177 =
            new BitSet(new long[]{0x000000000000AAD2L});
    public static final BitSet FOLLOW_conditionNot_in_conditionAnd213 =
            new BitSet(new long[]{0x000000000000AAD2L});
    public static final BitSet FOLLOW_conditionAnd2_in_conditionAnd218 =
            new BitSet(new long[]{0x000000000000AAD2L});
    public static final BitSet FOLLOW_conditionNot_in_conditionAnd240 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AND_in_conditionAnd2252 =
            new BitSet(new long[]{0x000000000000AAC0L});
    public static final BitSet FOLLOW_conditionNot_in_conditionAnd2255 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionNot_in_conditionAnd2261 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_conditionNot274 =
            new BitSet(new long[]{0x00000000000028C0L});
    public static final BitSet FOLLOW_conditionBase_in_conditionNot276 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NOT_in_conditionNot290 =
            new BitSet(new long[]{0x00000000000028C0L});
    public static final BitSet FOLLOW_conditionBase_in_conditionNot293 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionBase_in_conditionNot299 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_13_in_conditionBase311 =
            new BitSet(new long[]{0x000000000000AAC0L});
    public static final BitSet FOLLOW_conditionOr_in_conditionBase314 =
            new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_14_in_conditionBase316 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FIELD_NAME_in_conditionBase331 =
            new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_conditionBase334 =
            new BitSet(new long[]{0x00000000000008C0L});
    public static final BitSet FOLLOW_fieldValue_in_conditionBase337 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_fieldValue_in_conditionBase343 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FIELD_NAME_in_fieldValue366 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SINGLE_WORD_in_fieldValue379 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_EXACT_PHRASE_in_fieldValue385 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionAnd_in_synpred1_Query115 =
            new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_OR_in_synpred1_Query117 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionNot_in_synpred2_Query159 =
            new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_AND_in_synpred2_Query161 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionNot_in_synpred3_Query200 =
            new BitSet(new long[]{0x000000000000AAC0L});
    public static final BitSet FOLLOW_conditionNot_in_synpred3_Query202 =
            new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FIELD_NAME_in_synpred4_Query324 =
            new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_synpred4_Query326 =
            new BitSet(new long[]{0x0000000000000002L});
}
