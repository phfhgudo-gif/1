package com.thanpisitkub.mod.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiny Molang-style expression evaluator used by the particle definitions
 * (e.g. {@code math.random(0.5,1)}, {@code 4+v.particle_random_2*10}).
 * Pure Java (no Minecraft classes) so it can be unit-tested on its own.
 */
public final class Expr {

    /** Values an expression can read. */
    public static final class Ctx {
        public double age;          // v.particle_age (seconds)
        public double lifetime;     // v.particle_lifetime (seconds)
        public double size;         // v.size (written by the "size" curve)
        public double emitterAge;   // v.emitter_age (seconds)
        public double r1 = 0.5, r2 = 0.5, r3 = 0.5, r4 = 0.5; // v.particle_random_1..4
        public Random rng = new Random();
    }

    @FunctionalInterface
    private interface Node {
        double eval(Ctx c);
    }

    /** Variable / function names that were not understood (for diagnostics). */
    public static final Set<String> UNKNOWN = ConcurrentHashMap.newKeySet();

    private final String source;
    private final Node root;

    private Expr(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    public static Expr constant(double v) {
        return new Expr(Double.toString(v), c -> v);
    }

    public static Expr parse(String text) {
        if (text == null || text.isBlank()) return constant(0);
        Parser p = new Parser(text);
        Node n = p.parseExpression();
        p.skipWs();
        if (p.pos < p.s.length()) {
            throw new IllegalArgumentException("Unexpected '" + p.s.charAt(p.pos) + "' at " + p.pos + " in: " + text);
        }
        return new Expr(text, n);
    }

    public double eval(Ctx c) {
        return root.eval(c);
    }

    /** Evaluate with a blank context (for expressions that do not use variables). */
    public double evalPlain() {
        return root.eval(new Ctx());
    }

    @Override
    public String toString() {
        return source;
    }

    // ------------------------------------------------------------------ parser

    private static final class Parser {
        final String s;
        int pos = 0;

        Parser(String s) {
            this.s = s;
        }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        boolean eat(char ch) {
            skipWs();
            if (pos < s.length() && s.charAt(pos) == ch) {
                pos++;
                return true;
            }
            return false;
        }

        Node parseExpression() {
            Node left = parseTerm();
            while (true) {
                if (eat('+')) {
                    Node a = left, b = parseTerm();
                    left = c -> a.eval(c) + b.eval(c);
                } else if (eat('-')) {
                    Node a = left, b = parseTerm();
                    left = c -> a.eval(c) - b.eval(c);
                } else {
                    return left;
                }
            }
        }

        Node parseTerm() {
            Node left = parseUnary();
            while (true) {
                if (eat('*')) {
                    Node a = left, b = parseUnary();
                    left = c -> a.eval(c) * b.eval(c);
                } else if (eat('/')) {
                    Node a = left, b = parseUnary();
                    left = c -> {
                        double d = b.eval(c);
                        return d == 0 ? 0 : a.eval(c) / d;
                    };
                } else {
                    return left;
                }
            }
        }

        Node parseUnary() {
            if (eat('-')) {
                Node a = parseUnary();
                return c -> -a.eval(c);
            }
            if (eat('+')) return parseUnary();
            return parsePrimary();
        }

        Node parsePrimary() {
            skipWs();
            if (pos >= s.length()) throw new IllegalArgumentException("Unexpected end of expression: " + s);
            char ch = s.charAt(pos);
            if (ch == '(') {
                pos++;
                Node inner = parseExpression();
                if (!eat(')')) throw new IllegalArgumentException("Missing ')' in: " + s);
                return inner;
            }
            if (Character.isDigit(ch) || ch == '.') return parseNumber();
            if (Character.isLetter(ch) || ch == '_') return parseIdentifier();
            throw new IllegalArgumentException("Unexpected '" + ch + "' at " + pos + " in: " + s);
        }

        Node parseNumber() {
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                int save = pos;
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                if (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
                } else {
                    pos = save;
                }
            }
            final double v = Double.parseDouble(s.substring(start, pos));
            return c -> v;
        }

        Node parseIdentifier() {
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_' || s.charAt(pos) == '.')) pos++;
            String name = s.substring(start, pos).toLowerCase(Locale.ROOT);
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '(') {
                pos++;
                List<Node> args = new ArrayList<>();
                skipWs();
                if (!eat(')')) {
                    do {
                        args.add(parseExpression());
                    } while (eat(','));
                    if (!eat(')')) throw new IllegalArgumentException("Missing ')' after call to " + name + " in: " + s);
                }
                return function(name, args);
            }
            return variable(name);
        }
    }

    // --------------------------------------------------------------- variables

    private static Node variable(String name) {
        if (name.equals("math.pi")) return c -> Math.PI;
        String n = name;
        for (String prefix : new String[]{"variable.", "v.", "temp.", "t.", "query.", "q."}) {
            if (n.startsWith(prefix)) {
                n = n.substring(prefix.length());
                break;
            }
        }
        switch (n) {
            case "particle_random_1", "particle_random":
                return c -> c.r1;
            case "particle_random_2":
                return c -> c.r2;
            case "particle_random_3":
                return c -> c.r3;
            case "particle_random_4":
                return c -> c.r4;
            case "particle_age":
                return c -> c.age;
            case "particle_lifetime":
                return c -> c.lifetime;
            case "size":
                return c -> c.size;
            case "emitter_age":
                return c -> c.emitterAge;
            case "emitter_random_1", "emitter_random_2", "emitter_random_3", "emitter_random_4":
                return c -> 0.5;
            default:
                UNKNOWN.add(name);
                return c -> 0;
        }
    }

    // --------------------------------------------------------------- functions

    private static Node function(String name, List<Node> a) {
        String n = name.startsWith("math.") ? name.substring(5) : name;
        switch (n) {
            case "random": {
                if (a.size() == 0) return c -> c.rng.nextDouble();
                if (a.size() == 2) {
                    Node lo = a.get(0), hi = a.get(1);
                    return c -> {
                        double l = lo.eval(c), h = hi.eval(c);
                        return l + c.rng.nextDouble() * (h - l);
                    };
                }
                break;
            }
            case "random_integer": {
                if (a.size() == 2) {
                    Node lo = a.get(0), hi = a.get(1);
                    return c -> {
                        double l = lo.eval(c), h = hi.eval(c);
                        return Math.floor(l + c.rng.nextDouble() * (h - l + 1));
                    };
                }
                break;
            }
            case "sin":
                if (a.size() == 1) return c -> Math.sin(Math.toRadians(a.get(0).eval(c)));
                break;
            case "cos":
                if (a.size() == 1) return c -> Math.cos(Math.toRadians(a.get(0).eval(c)));
                break;
            case "abs":
                if (a.size() == 1) return c -> Math.abs(a.get(0).eval(c));
                break;
            case "sqrt":
                if (a.size() == 1) return c -> Math.sqrt(Math.max(0, a.get(0).eval(c)));
                break;
            case "floor":
                if (a.size() == 1) return c -> Math.floor(a.get(0).eval(c));
                break;
            case "ceil":
                if (a.size() == 1) return c -> Math.ceil(a.get(0).eval(c));
                break;
            case "round":
                if (a.size() == 1) return c -> Math.rint(a.get(0).eval(c));
                break;
            case "trunc":
                if (a.size() == 1) return c -> {
                    double v = a.get(0).eval(c);
                    return v < 0 ? Math.ceil(v) : Math.floor(v);
                };
                break;
            case "exp":
                if (a.size() == 1) return c -> Math.exp(a.get(0).eval(c));
                break;
            case "ln":
                if (a.size() == 1) return c -> Math.log(a.get(0).eval(c));
                break;
            case "min":
                if (a.size() == 2) return c -> Math.min(a.get(0).eval(c), a.get(1).eval(c));
                break;
            case "max":
                if (a.size() == 2) return c -> Math.max(a.get(0).eval(c), a.get(1).eval(c));
                break;
            case "pow":
                if (a.size() == 2) return c -> Math.pow(a.get(0).eval(c), a.get(1).eval(c));
                break;
            case "mod":
                if (a.size() == 2) return c -> {
                    double d = a.get(1).eval(c);
                    return d == 0 ? 0 : a.get(0).eval(c) % d;
                };
                break;
            case "clamp":
                if (a.size() == 3) return c -> Math.max(a.get(1).eval(c), Math.min(a.get(2).eval(c), a.get(0).eval(c)));
                break;
            case "lerp":
                if (a.size() == 3) return c -> {
                    double x = a.get(0).eval(c), y = a.get(1).eval(c), t = a.get(2).eval(c);
                    return x + (y - x) * t;
                };
                break;
            default:
                break;
        }
        throw new IllegalArgumentException("Unsupported function " + name + " with " + a.size() + " argument(s)");
    }
}
