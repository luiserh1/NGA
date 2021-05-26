package es.upv.opponentmodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import agents.anac.y2019.harddealer.math3.util.MultidimensionalCounter.Iterator;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.utility.AdditiveUtilitySpace;
import negotiator.boaframework.opponentmodel.tools.UtilitySpaceAdapter;

public class SimpleFrequencyModel extends OpponentModel {
	
	// Variable para almacenar la última oferta del oponente
	private Bid lastOpponentBid;
	// Variable contador que almacena el número de ofertas recibidas por el agente
	private int opponentBidCount;
	// Constante empleada para actualizar los valores de los pesos
	private final double DELTA = 0.01;
	// Variable tipo diccionario que relaciona los atributos de la negociación con sus pesos estimados
	HashMap<Integer, Double> weightsPerAttribute;
	// Variable tipo diccionario ania¡dado que se empleará para contear el número de veces que
	// se ha asignado un valor a cada tipo de atributo
	HashMap<Integer, HashMap<Value, Integer>> valuesPerAttribute;

	@Override
	public String getName() {
		return "Simple Frequency Model";
	}
	
	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		super.init(negotiationSession, parameters);
		
		lastOpponentBid = null;
		opponentBidCount = 0;
		weightsPerAttribute = new HashMap<Integer, Double>();
		valuesPerAttribute = new HashMap<Integer, HashMap<Value, Integer>>();
		List<Issue> issuesList = negotiationSession.getDomain().getIssues();
		int issuesCount = issuesList.size();
		double initWeight = 1.0f / issuesCount;
		for (Issue is : issuesList)
		{
			int index = is.getNumber();
			weightsPerAttribute.put(index, initWeight);
			valuesPerAttribute.put(index, new HashMap<Value, Integer>());
			IssueDiscrete isD = (IssueDiscrete)is;
			for (Value val : isD.getValues())
				valuesPerAttribute.get(index).put(val, 0);
		}
	}
	
	@Override
	protected void updateModel(Bid bid, double time) {
		 opponentBidCount++;
		 List<Issue> issuesList = bid.getIssues();
			for (Issue is : issuesList)
			{
				int index = is.getNumber();
				Value newVal = bid.getValue(index);
				// Actualizar el uso del valor para el Issue
				int lastCount = valuesPerAttribute.get(index).get(newVal);
				valuesPerAttribute.get(index).put(newVal, lastCount+1);
				// Actualizar el valor de los pesos
				if (newVal == lastOpponentBid.getValue(index))
				{
					double lastWeight = weightsPerAttribute.get(index);
					weightsPerAttribute.put(index, lastWeight + DELTA);
				}
			}
	}
	
	@Override
	public double getBidEvaluation(Bid bid) {
		// Normalización de los pesos
		double sum = 0;
		for (double w : weightsPerAttribute.values())
			sum += w;
		for (int index : weightsPerAttribute.keySet())
			weightsPerAttribute.put(index,
									weightsPerAttribute.get(index) / sum);
		// Cálculo de la utilidad
		double u = 0;
		int r = opponentBidCount;
		for (Issue is : bid.getIssues())
		{
			int index = is.getNumber();
			IssueDiscrete isD = (IssueDiscrete)is;
			double weight = weightsPerAttribute.get(index);
			for (Value val : isD.getValues())
			{
				int valUses = valuesPerAttribute.get(index).get(val);
				double v_i = valUses / r;
				u += weight * v_i;
			}
		}
		return u;
	}
	
	@Override
	public AdditiveUtilitySpace getOpponentUtilitySpace() {
		AdditiveUtilitySpace utilitySpace = new UtilitySpaceAdapter(this, this.negotiationSession.getDomain());
		return utilitySpace;
	}
	
	@Override
	public Set<BOAparameter> getParameterSpec(){
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		/* Aquí describe los parámetros que necesita el algoritmo de aprendizaje. Ejemplos:
			set.add(new BOAparameter("n", 20.0, "The number of own best offers to be used for genetic operations"));
			set.add(new BOAparameter("n_opponent", 20.0, "The number of opponent's best offers to be used for genetic operations"));
		*/
		return set;
	}

}
