package StationSim

import io.improbable.keanu.algorithms.mcmc.MetropolisHastings
import io.improbable.keanu.network.BayesianNetwork
import io.improbable.keanu.vertices.dbl.probabilistic.ExponentialVertex
import io.improbable.keanu.vertices.dbl.probabilistic.SmoothUniformVertex
import io.improbable.keanu.vertices.dbl.probabilistic.UniformVertex
import io.improbable.keanu.vertices.intgr.probabilistic.UniformIntVertex
import io.improbable.keanu.vertices.dbltensor.KeanuRandom
import java.util.Arrays.asList


fun findSwitchpoint() {
    val obs = asList(0,0,0,1,1,2,4,5,5,6,6,6,8,9,8,9,9,8,9,7,7,6,5,6,4,4,3,2,2,1,1,0,0,0)
    val meanCount = obs.average()
    val countLength = obs.size

    val alpha = 1.0 / meanCount
    val lambdaOne = ExponentialVertex(alpha, alpha) //Why two inputs?
    val lambdaTwo = ExponentialVertex(alpha, alpha) //Why two inputs?
    //val lambdaThree = ExponentialVertex(alpha, alpha) //Why two inputs?

    val tauOne = UniformIntVertex(0, countLength)
    val tauTwo = UniformIntVertex(0, countLength)

    for (step in 0..countLength) {
        
    }

    //println(tauOne.sample(KeanuRandom.getDefaultRandom()))

    val net = BayesianNetwork(tauOne.connectedGraph)
    val posteriorSamples = MetropolisHastings.getPosteriorSamples(net, net.latentVertices, 10000, KeanuRandom.getDefaultRandom())

    val results = posteriorSamples.drop(1000).downSample(5).get(tauOne)
    println(results.asList())
}


fun main(args: Array<String>) {
    findSwitchpoint()
}

/*
It uses data of yearly number of coal mining accidents in the UK. The aim is to infer the "switch point" year, when the rate of accidents changes to a lower rate (this could be due to the introduction of a new safety process). The number of accidents in each year is modelled by a poisson distribution, with different rates before and after this switch point. These different rates are hyper parameters to the Poisson distribution, and are modelled as exponential vertexes.

It uses Metropolis Hastings Monte Carlo to sample the posterior distribution of the switch point, since this uses non continuous Integer Vertices you cannot use the MAP method.
 */