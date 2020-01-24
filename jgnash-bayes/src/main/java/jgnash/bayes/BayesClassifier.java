/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.bayes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Naive Bayes BayesClassifier.
 * Modeled after classifier presented in "Programming Collective Intelligence" by Toby Segaran
 *
 * @param <E> the type of mapped value
 * 
 * @author Craig Cavanaugh
 */
public class BayesClassifier<E> {

    /**
     * Default class if not determinate
     */
    private final E defaultClass;

    private static final double ASSUMED_PROBABILITY = 0.5;
    private static final double THRESHOLD = 1.0;
    private static final double WEIGHT = 1.0;
    private static final String WHITE_SPACE_REGEX = "[,\\s]+";
    // private final static String NUMBERS_REGEX = "(?>-?\\d+(?:[\\./]\\d+)?)";

    private final Map<String, Map<E, Integer>> featureCounter = new HashMap<>();
    private final Map<E, Integer> classCounter = new HashMap<>();
    private final Pattern whiteSpacePattern;

    /**
     * Constructor
     * 
     * @param defaultClass the mapped type
     */
    public BayesClassifier(final E defaultClass) {
        this.defaultClass = defaultClass;
        whiteSpacePattern = Pattern.compile(WHITE_SPACE_REGEX);
    }

    private void incrementFeature(final String feature, final E classification) {
        Map<E, Integer> featureMap = featureCounter.get(feature);

        if (featureMap == null) {
            featureMap = new HashMap<>();
            featureMap.put(classification, 1);
            featureCounter.put(feature, featureMap);
            return;
        }

        Integer count = featureMap.get(classification);
        count = (count == null) ? 1 : count + 1;
        featureMap.put(classification, count);
    }

    /**
     * Support method to increment a classification
     *
     * @param classification classification to increment
     */
    private void incrementClass(final E classification) {
        Integer count = classCounter.get(classification);
        count = (count == null) ? 1 : count + 1;
        classCounter.put(classification, count);
    }

    private int getClassCount(final E classification) {
        Integer count = classCounter.get(classification);

        return (count != null) ? count : 0;
    }

    /**
     * Gets the number of times the feature as occurred in the classification
     *
     * @param feature        feature to count
     * @param classification class
     * @return occurrence count
     */
    private int getFeatureCount(final String feature, final E classification) {
        Map<E, Integer> featureMap = featureCounter.get(feature);

        if (featureMap == null) {
            return 0;
        }
        Integer count = featureMap.get(classification);

        return (count != null) ? count : 0;
    }

    private int getFeatureCount(final String feature) {
        Map<E, Integer> featureMap = featureCounter.get(feature);
        int count = 0;

        if (featureMap == null) {
            return count;
        }               
        
        for (Entry<E, Integer> entry : featureMap.entrySet()) {
            count += entry.getValue();
        }                    

        return count;
    }

    private double getFeatureProbability(final String feature, final E classification) {
        int count = getClassCount(classification);
        return (count == 0) ? 0.0 : (double) getFeatureCount(feature, classification) / count;
    }

    private double getWeightedProbability(final String feature, final E classification) {
        double probability = getFeatureProbability(feature, classification);
        int totals = getFeatureCount(feature);
        return (WEIGHT * ASSUMED_PROBABILITY + totals * probability) / (WEIGHT + totals);
    }

    private double getClassProbability(final String item, final E classification) {
        double probability = 1;

        for (final String feature : whiteSpacePattern.split(item)) {
            probability *= getWeightedProbability(feature, classification);
        }

        return (double) classCounter.get(classification) / classCounter.size() * probability;
    }

    private void train(final Collection<String> features, final E classification) {
        for (String feature : features) {
            incrementFeature(feature, classification);
        }
        incrementClass(classification);
    }

    /**
     * Trains the classifier
     * 
     * @param item training string
     * @param classification object being classified
     */
    public void train(final String item, final E classification) {
        train(Arrays.asList(whiteSpacePattern.split(item.toLowerCase(Locale.getDefault()))), classification);
    }

    /**
     * Returns the best probabilistic match 
     * 
     * @param item String data to match
     * @return best possible match
     */
    public E classify(final String item) {
        E bestClass = defaultClass;
        double classProb;
        double bestProbability;
        double max = 0;

        Map<E, Double> probabilities = new HashMap<>();

        // find the category with the highest probability
        for (final E classification : classCounter.keySet()) {
            classProb = getClassProbability(item.toLowerCase(Locale.getDefault()), classification);
            probabilities.put(classification, classProb);
            if (classProb > max) {
                max = classProb;
                bestClass = classification;
            }
        }
                       
        // make sure the probability exceeds
        for (final Entry<E, Double> entry : probabilities.entrySet()) {
            if (!entry.getKey().equals(bestClass)) {
                classProb = entry.getValue();
                bestProbability = probabilities.get(bestClass);
                
                if (classProb * THRESHOLD >= bestProbability) {
                    return defaultClass;
                }
            }
        }       

        return bestClass;
    }
}