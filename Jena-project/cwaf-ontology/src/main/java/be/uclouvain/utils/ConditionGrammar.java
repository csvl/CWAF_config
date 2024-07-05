package be.uclouvain.utils;

import org.apache.jena.sparql.pfunction.library.seq;

import norswap.autumn.Grammar;

public class ConditionGrammar extends Grammar{
    
    { ws = usual_whitespace; }


    //Values taken from https://httpd.apache.org/docs/2.4/expr.html#vars
    public rule varname = choice("HTTP_ACCEPT",
                                "HTTP_COOKIE",
                                "HTTP_FORWARDED",
                                "HTTP_HOST",
                                "HTTP_PROXY_CONNECTION",
                                "HTTP_REFERER",
                                "HTTP_USER_AGENT",
                                //request variables
                                "REQUEST_METHOD",
                                "REQUEST_SCHEME",
                                "REQUEST_URI",
                                "DOCUMENT_URI",
                                "REQUEST_FILENAME",
                                "SCRIPT_FILENAME",
                                "LAST_MODIFIED",
                                "SCRIPT_USER",
                                "SCRIPT_GROUP",
                                "PATH_INFO",
                                "QUERY_STRING",
                                "IS_SUBREQ",
                                "THE_REQUEST",
                                "REMOTE_ADDR",
                                "REMOTE_PORT",
                                "REMOTE_HOST",
                                "REMOTE_USER",
                                "REMOTE_IDENT",
                                "SERVER_NAME",
                                "SERVER_PORT",
                                "SERVER_ADMIN",
                                "SERVER_PROTOCOL",
                                "DOCUMENT_ROOT",
                                "AUTH_TYPE",
                                "CONTENT_TYPE",
                                "HANDLER",
                                "HTTP2",
                                "HTTPS",
                                "IPV6",
                                "REQUEST_STATUS",
                                "REQUEST_LOG_ID",
                                "CONN_LOG_ID",
                                "CONN_REMOTE_ADDR",
                                "CONTEXT_PREFIX",
                                "CONTEXT_DOCUMENT_ROOT",
                                //misc
                                "TIME_YEAR",
                                "TIME_MON",
                                "TIME_DAY",
                                "TIME_HOUR",
                                "TIME_MIN",
                                "TIME_SEC",
                                "TIME_WDAY",
                                "TIME",
                                "SERVER_SOFTWARE",
                                "API_VERSION");

    public rule funcname = choice("req",
                                "http",
                                "req_novary",
                                "resp",
                                "reqenv",
                                "osenv",
                                "note",
                                "env",
                                "tolower",
                                "toupper",
                                "escape",
                                "unescape",
                                "base64",
                                "unbase64",
                                "md5",
                                "sha1",
                                "file",
                                "filesize",
                                "ldap");
    
    public rule DOLLAR = word("$");
    public rule EQUALS = word("==");
    public rule NOT_EQUALS = word("!=");
    public rule GREATER_THAN = word(">");
    public rule GREATER_THAN_EQUALS = word(">=");
    public rule LESS_THAN = word("<");
    public rule LESS_THAN_EQUALS = word("<=");

    public rule INT_EQUAL = word("eq");
    public rule DASHED_INT_EQUAL = word("-eq");
    public rule INT_NOT_EQUAL = word("ne");
    public rule DASHED_INT_NOT_EQUAL = word("-ne");
    public rule INT_GREATER_THAN = word("gt");
    public rule DASHED_INT_GREATER_THAN = word("-gt");
    public rule INT_GREATER_THAN_EQUALS = word("ge");
    public rule DASHED_INT_GREATER_THAN_EQUALS = word("-ge");
    public rule INT_LESS_THAN = word("lt");
    public rule DASHED_INT_LESS_THAN = word("-lt");
    public rule INT_LESS_THAN_EQUALS = word("le");
    public rule DASHED_INT_LESS_THAN_EQUALS = word("-le");

    public rule unary_op = choice("-d",
                                    "-e",
                                    "-f",
                                    "-s",
                                    "-L",
                                    "-h",
                                    "-F",
                                    "-U",
                                    "-A",
                                    "-n",
                                    "-z",
                                    "-T",
                                    "-R");

    public rule binary_op = choice(EQUALS,
                                    NOT_EQUALS,
                                    GREATER_THAN,
                                    GREATER_THAN_EQUALS,
                                    LESS_THAN,
                                    LESS_THAN_EQUALS,
                                    word("=~"),
                                    word("!~"),
                                    INT_EQUAL,
                                    DASHED_INT_EQUAL,
                                    INT_NOT_EQUAL,
                                    DASHED_INT_NOT_EQUAL,
                                    INT_GREATER_THAN,
                                    DASHED_INT_GREATER_THAN,
                                    INT_GREATER_THAN_EQUALS,
                                    DASHED_INT_GREATER_THAN_EQUALS,
                                    INT_LESS_THAN,
                                    DASHED_INT_LESS_THAN,
                                    INT_LESS_THAN_EQUALS,
                                    DASHED_INT_LESS_THAN_EQUALS,
                                    word("-ipmatch"),
                                    word("-strmatch"),
                                    word("-strcmatch"),
                                    word("-fnmatch"));

    public rule function = seq(funcname, '(', lazy(() -> this.word), ')');
    public rule rebackref = seq(DOLLAR, range(0, 9));
    public rule variable = choice(seq("%{", varname , '}'));    
    public rule integer = choice('0', digit.at_least(1));

    public rule stringpart = choice(alphanum, variable, rebackref);
    public rule string = choice(stringpart, seq(stringpart, lazy(() -> this.string)));
    public rule regex = seq('/', string, '/', word("i").opt());

    public rule word = choice(seq(lazy(() -> this.word), ".", lazy(() -> this.word)),
                            integer,
                            seq("'", string, "'"),
                            seq('"', string, '"'),
                            variable,
                            rebackref,
                            function);

    public rule wordlist = choice(seq(word, ',', lazy(() -> this.wordlist)),
                                word);

    public rule integercomp = choice(seq(word, INT_EQUAL, word),
                                    seq(word, DASHED_INT_EQUAL, word),
                                    seq(word, INT_NOT_EQUAL, word),
                                    seq(word, DASHED_INT_NOT_EQUAL, word),
                                    seq(word, INT_GREATER_THAN, word),
                                    seq(word, DASHED_INT_GREATER_THAN, word),
                                    seq(word, INT_GREATER_THAN_EQUALS, word),
                                    seq(word, DASHED_INT_GREATER_THAN_EQUALS, word),
                                    seq(word, INT_LESS_THAN, word),
                                    seq(word, DASHED_INT_LESS_THAN, word),
                                    seq(word, INT_LESS_THAN_EQUALS, word),
                                    seq(word, DASHED_INT_LESS_THAN_EQUALS, word));

    public rule stringcomp = choice(seq(word, EQUALS, word),
                                    seq(word, NOT_EQUALS, word),
                                    seq(word, GREATER_THAN, word),
                                    seq(word, GREATER_THAN_EQUALS, word),
                                    seq(word, LESS_THAN, word),
                                    seq(word, LESS_THAN_EQUALS, word));

    public rule comp = choice(integercomp,
                                stringcomp,
                                seq(unary_op, word),
                                seq( word, binary_op, word),
                                seq(word, "in", '{', wordlist, '}'),
                                seq(word, "=~", regex),
                                seq(word, "!~", regex));

    public rule expr = choice(comp,
                            seq('(', lazy(() -> this.expr), ')'),
                            seq('!', lazy(() -> this.expr)),
                            left_expression().operand(lazy(() -> this.expr)).infix(str("&&")),
                            left_expression().operand(lazy(() -> this.expr)).infix(str("||")),
                            word("true"), word("false"));

    public rule root = expr;

    @Override
    public rule root() {
        return root;
    }
    
}
