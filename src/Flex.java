import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rationals.Automaton;
import rationals.NoSuchStateException;
import rationals.State;
import rationals.Transition;
import rationals.transformations.Concatenation;
import rationals.transformations.Union;
import recoder.CrossReferenceServiceConfiguration;
import recoder.ParserException;
import recoder.convenience.TreeWalker;
import recoder.io.PropertyNames;
import recoder.io.SourceFileRepository;
import recoder.java.CompilationUnit;
import recoder.java.ProgramElement;
import recoder.java.StatementBlock;
import recoder.java.declaration.FieldSpecification;
import recoder.java.declaration.MethodDeclaration;
import recoder.java.declaration.VariableSpecification;
import recoder.java.expression.operator.CopyAssignment;
import recoder.java.expression.operator.New;
import recoder.java.reference.MethodReference;
import recoder.java.statement.Case;
import recoder.java.statement.Do;
import recoder.java.statement.Else;
import recoder.java.statement.For;
import recoder.java.statement.If;
import recoder.java.statement.Switch;
import recoder.java.statement.While;

public class Flex {

	private String dir;
	static Set<ProgramElement> processed = new HashSet<ProgramElement>();
	String freeTransition = "";

	public Flex(String dir, String klass, String method, String var) {
		this.dir = dir;
		CrossReferenceServiceConfiguration crsc = new CrossReferenceServiceConfiguration();
		crsc.getProjectSettings().setProperty(PropertyNames.INPUT_PATH, dir.trim());
		crsc.getProjectSettings().ensureSystemClassesAreInPath();
		SourceFileRepository sfr = crsc.getSourceFileRepository();
		List<CompilationUnit> cul = null;
		try {
			cul = sfr.getAllCompilationUnitsFromPath();
		} catch (ParserException e) {
//			e.printStackTrace();
		}
		// crsc.getChangeHistory().updateModel();
		for (CompilationUnit cunit : cul) {
			if (cunit.getName().endsWith(klass)) {

				TreeWalker tw = new TreeWalker(cunit);
				tw = new TreeWalker(getDeclarationMethod(tw.getProgramElement(), method));
				System.out.println(tw.getProgramElement().toSource());

				Automaton a = new Automaton();
				try {
					a = generate(tw.getProgramElement(), var);
				} catch (NoSuchStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.err.println(a);
				while (tw.next()) {
					if (tw.getProgramElement() instanceof FieldSpecification) {
						FieldSpecification vb = (FieldSpecification) tw.getProgramElement();
						System.out.println(vb.getName());
					}
				}
			}

		}

	}

	public Automaton generate(ProgramElement p, String objectView) throws NoSuchStateException {
		Automaton automato = new Automaton();
		automato.addTransition(
				new Transition(automato.addState(true, false), freeTransition, automato.addState(false, true)));
		TreeWalker tw = new TreeWalker(p);

		Concatenation concatenation = new Concatenation();

		while (tw.next()) {
			ProgramElement pe = tw.getProgramElement();

			if (jaProcessou(pe)) {
				continue;
			}

			if (pe instanceof New) {

				New m = (New) pe;
				if (isObjectView(m.getASTParent().getChildAt(0).toSource(), objectView)) {
					Automaton a = new Automaton();
					State s0 = a.addState(true, false);
					State s1 = a.addState(false, true);
					a.addTransition(new Transition(s0, m.toSource(), s1));
					automato = concatenation.transform(automato, a);
					processed.add(pe);
				}
			}

			if (pe instanceof MethodReference) {
				MethodReference m = (MethodReference) pe;
				if (isObjectView(m.getChildAt(0).toSource(), objectView)) {
					Automaton a = new Automaton();
					State s0 = a.addState(true, false);
					State s1 = a.addState(false, true);
					a.addTransition(new Transition(s0, m.getIdentifier().toSource(), s1));

					automato = concatenation.transform(automato, a);
					processed.add(pe);
				}
			}

			if (pe instanceof If) {
				// condition
				Automaton condition = generate(((If) pe).getExpression(), objectView);
				if (!isEmpty(condition)) {
					automato = concatenation.transform(automato, condition);
				}
				tw.next();
				ProgramElement p2 = tw.getProgramElement();
				p2 = ((If) pe).getThen();
				// if
				Automaton automatoIf = generate(p2, objectView);
				processed.add(pe);
				If ifState = ((If) pe);
				Else elseBlock = ifState.getElse();
				Map<State, State> map = new HashMap<>();
				if (elseBlock != null) {
					// else
					Automaton automatoElse = generate(elseBlock, objectView);
					if (!isEmpty(automatoElse)) {
						State initial = getAInitial(automatoIf);
						State terminal = getATerminal(automatoIf);
						Iterator<State> i2 = automatoElse.states().iterator();
						while (i2.hasNext()) {
							State e = i2.next();
							State newS = e.isInitial() ? initial : automatoIf.addState(false, false);
							if (e.isTerminal()) {
								newS = terminal;
							}
							map.put(e, newS);
						}
						Iterator<Transition> trans = automatoElse.delta().iterator();
						while (trans.hasNext()) {
							Transition e = trans.next();
							automatoIf.addTransition(new Transition(map.get(e.start()), e.label(), map.get(e.end())));
						}
						processed.add(elseBlock);
					}else {
						automatoIf.addTransition(
								new Transition(getAInitial(automatoIf), freeTransition, getATerminal(automatoIf)));
					}
				} 
				if (!isEmpty(automatoIf)) {
					automato = concatenation.transform(automato, automatoIf);
				}
			}
		}
		return automato;

	}

	public boolean isState(ProgramElement p) {
		if (p instanceof VariableSpecification) {
			return true;
		}
		if (p instanceof MethodReference) {
			return true;
		}
		if (p instanceof CopyAssignment) {
			return true;
		}
		if (p instanceof Do) {
			return true;
		}
		if (p instanceof For) {
			return true;
		}
		if (p instanceof While) {
			return true;
		}
		if (p instanceof If) {
			return true;
		}
		if (p instanceof Else) {
			return true;
		}
		if (p instanceof StatementBlock) {
			return true;
		}
		if (p instanceof Switch) {
			return true;
		}
		if (p instanceof Case) {
			return true;
		}

		return false;
	}

	public boolean isObjectView(String source, String var) {
		source = source.trim().replace("this.", "").replace("super.", "");
		return source.trim().equals(var.trim());
	}

	// return a method with the name @methodName in the class p
	public ProgramElement getDeclarationMethod(ProgramElement p, String methodName) {
		TreeWalker tw = new TreeWalker(p);
		while (tw.next()) {
			ProgramElement pe = tw.getProgramElement();
			if (pe instanceof MethodDeclaration) {
				MethodDeclaration md = (MethodDeclaration) pe;
				if (md.getName().equals(methodName))
					return pe;
			}
		}
		return null;
	}

	public boolean jaProcessou(ProgramElement p) {
		try {
			if (processed.contains(p.getASTParent()) || processed.contains(p)) {
				return true;
			}
			jaProcessou(p.getASTParent());

		} catch (NullPointerException e) {
			return false;// TODO: handle exception
		}
		return false;
	}

	private boolean isEmpty(Automaton automato) {
		return automato.alphabet().size() == 1;
	}

	private State getAInitial(Automaton automaton) {
		return (State) automaton.initials().toArray()[0];
	}

	private State getATerminal(Automaton automaton) {
		return (State) automaton.terminals().toArray()[0];
	}

}
