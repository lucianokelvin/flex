import rationals.Automaton;
import rationals.NoSuchStateException;
import rationals.State;
import rationals.Transition;
import rationals.transformations.Concatenation;

public class TeteAutomata {

	public static void main(String[] args) throws NoSuchStateException {
		Automaton automaton = new Automaton();
		State initial = automaton.addState(true, false);
		State finalState = automaton.addState(false, false);
		State terceiro = automaton.addState(false, true);
		
		Transition transition = new Transition(initial,"Primeiro" , finalState);
		Transition transition2 = new Transition(finalState,"Segundo" , terceiro);
		automaton.addTransition(transition);
		automaton.addTransition(transition2);
	
		
		Automaton automaton2 = new Automaton();
		State s = automaton2.addState(true, false);
		State s2 = automaton2.addState(false, true);
		automaton2.addTransition(new Transition(s, "Terceiro", s2));
		
		Concatenation concatenation = new Concatenation();
		
		Automaton t = concatenation.transform(automaton2, automaton);
		
				
		System.out.println(t.toString());
	}
	
}

