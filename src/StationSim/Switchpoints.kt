package StationSim

import io.improbable.keanu.algorithms.mcmc.MetropolisHastings
import io.improbable.keanu.network.BayesianNetwork
import io.improbable.keanu.randomfactory.RandomDoubleFactory
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.GreaterThanVertex
import io.improbable.keanu.vertices.dbl.DoubleVertex
import io.improbable.keanu.vertices.dbl.nonprobabilistic.CastDoubleVertex
import io.improbable.keanu.vertices.dbl.nonprobabilistic.ConstantDoubleVertex
import io.improbable.keanu.vertices.dbl.probabilistic.ExponentialVertex
import io.improbable.keanu.vertices.dbl.probabilistic.GaussianVertex
import io.improbable.keanu.vertices.intgr.probabilistic.UniformIntVertex
import io.improbable.keanu.vertices.dbltensor.KeanuRandom
import io.improbable.keanu.vertices.generic.nonprobabilistic.ConstantVertex
import io.improbable.keanu.vertices.generic.nonprobabilistic.IfVertex
import io.improbable.keanu.vertices.intgr.probabilistic.PoissonVertex
import java.util.stream.Collectors
import java.util.stream.IntStream


fun findSwitchpoint() {
    val data = hashMapOf<Int, Int>(
            0 to 0,
            1 to 0,
            2 to 0,
            3 to 1,
            4 to 1,
            5 to 2,
            6 to 4,
            7 to 5,
            8 to 5,
            9 to 6,
            10 to 6,
            11 to 6,
            12 to 84,
            13 to 93,
            14 to 87,
            15 to 91,
            16 to 90,
            17 to 89,
            18 to 90,
            19 to 79,
            20 to 79,
            21 to 6,
            22 to 5,
            23 to 6,
            24 to 4,
            25 to 4,
            26 to 3,
            27 to 2,
            28 to 2,
            29 to 1,
            30 to 1,
            31 to 0,
            32 to 0,
            33 to 0)

    val meanCount = data.values.average()
    val countLength = data.size
    val startStep : Int = data.keys.min()!!
    val endStep : Int = data.keys.max()!!

    val alpha = ConstantDoubleVertex(1.0 / meanCount)
    val lambdaOne = ExponentialVertex(alpha, alpha) //Why two inputs?
    val lambdaTwo = ExponentialVertex(alpha, alpha) //Why two inputs?
    //val lambdaThree = ExponentialVertex(alpha, alpha) //Why two inputs?

    val tauOne = UniformIntVertex(0, countLength)
    //val tauTwo = UniformIntVertex(0, countLength)


    val rates = IntStream.range(startStep, endStep).boxed()
            .map<ConstantVertex<Int>>( { ConstantVertex(it) } )
            .map { step ->
                val switchpointGreaterThanStep = GreaterThanVertex<Int, Int>(tauOne, step)
                IfVertex<Double>(switchpointGreaterThanStep, lambdaOne, lambdaTwo)
            }

    //for (x : IfVertex<Double> in rates) {
    //    println(x.value)
    //}

    val crowding = rates
            .map<CastDoubleVertex> { CastDoubleVertex(it) }
            .map<PoissonVertex> {rate -> PoissonVertex(rate) }
            .collect(Collectors.toList())  // future versions of kotlin won't need collect apparently

    println(lambdaOne.connectedGraph)

    //for (x in crowding) {
    //    println(x.value)
    //}

    println("Length of crowding: " + crowding.size)
    for (step in 0..countLength - 2) {
        val observation = data.getValue(step)
        println(observation)
        crowding[step].observe(observation)
    }

    val net = BayesianNetwork(lambdaOne.connectedGraph)
    val posteriorSamples = MetropolisHastings.getPosteriorSamples(net, net.latentVertices, 10000, KeanuRandom.getDefaultRandom())

    val results = posteriorSamples.drop(1000).downSample(5).get(tauOne)
    println(results.asList())
    println("Switchpoint at:" + results.mode)
}


fun main(args: Array<String>) {
    findSwitchpoint()
}

/*
It uses data of yearly number of coal mining accidents in the UK. The aim is to infer the "switch point" year, when the rate of accidents changes to a lower rate (this could be due to the introduction of a new safety process). The number of accidents in each year is modelled by a poisson distribution, with different rates before and after this switch point. These different rates are hyper parameters to the Poisson distribution, and are modelled as exponential vertexes.

It uses Metropolis Hastings Monte Carlo to sample the posterior distribution of the switch point, since this uses non continuous Integer Vertices you cannot use the MAP method.
 */