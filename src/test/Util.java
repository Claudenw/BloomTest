package test;

public class Util {

	public static double getMissProbability(BloomFilter target,
			BloomFilter candidate) {
		return getMissProbability(target.getWidth(), target.getHammingWeight(),
				candidate.getHammingWeight());
	}

	/*
	 * 
	 * Hypergeometric Formula. Suppose a population consists of N items, k of
	 * which are successes. And a random sample drawn from that population
	 * consists of n items, x of which are successes. Then the hypergeometric
	 * probability is:
	 * 
	 * h(x; N, n, k) = [ kCx ] [ N-kCn-x ] / [ NCn ]
	 */

	public static double getMissProbability(int width, int targetWeight,
			int candidateWeight) {
		// we want to know the probability that one of the target bits is in the
		// open space
		// in the candidate.
		int N = width;
		int k = candidateWeight;
		int n = targetWeight;
		return hypergeometricProbability(N, k, n, targetWeight);
		
	}

	/*
	 * 
	 * Hypergeometric Formula. Suppose a population consists of N items, k of
	 * which are successes. And a random sample drawn from that population
	 * consists of n items, x of which are successes. Then the hypergeometric
	 * probability is:
	 * 
	 * h(x; N, n, k) = [ kCx ] [ N-kCn-x ] / [ NCn ]
	 */

	public static double hypergeometricProbability(int N, int k, int n, int x )
	{
		return	 Math.pow(Math.E,
					logCombination(k, x) + logCombination((N - k), (n - x))
							- logCombination(N, n));
	}
	
	/*
	 * 
	 * Hypergeometric Formula. Suppose a population consists of N items, k of
	 * which are successes. And a random sample drawn from that population
	 * consists of n items, x of which are successes. Then the hypergeometric
	 * probability is:
	 * 
	 * h(x; N, n, k) = [ kCx ] [ N-kCn-x ] / [ NCn ]
	 */

	public static double hypergeometricSeriesProbability(int N, int k, int n, int minX, int maxX)
	{
		Double retval = 0.0;
	
		for (int x=minX;x<=maxX;x++)
		{
			retval += hypergeometricProbability( N,  k,  n,  x );
		}
		return retval;
	}
	/*
	 * 
	 * Computing the number of combinations. The number of Combinations of n
	 * objects taken r at a time is nCr = n(n - 1)(n - 2) ... (n - r + 1)/r! =
	 * n! / r!(n - r)! = nPr / r!
	 */
	public static double combination(int n, int r) {
		return Math.pow(Math.E,
				(logFactorial(n) - (logFactorial(r) + logFactorial(n - r))));
	}

	public static double logCombination(int n, int r) {
		return logFactorial(n) - (logFactorial(r) + logFactorial(n - r));
	}

	public static double logFactorial(int n) {
		double fact = Math.log(1); // this will be the result
		for (int i = 1; i <= n; i++) {
			fact += Math.log(i);
		}
		return fact;
	}

	public static double factorial(int n) {
		double fact = 1.0; // this will be the result
		for (int i = 1; i <= n; i++) {
			fact *= i;
		}
		return fact;
	}

	public static void main(String[] args) {

		double retval = 0.0;

		retval = factorial(5);
		System.out.println(String.format("%s == %s", retval, 120));
		retval = logFactorial(5);
		System.out.println(String.format("%s == %s", retval, Math.log(120)));
		System.out.println(String.format("%s == %s", Math.pow(Math.E, retval),
				120));

		retval = combination(26, 2);
		System.out.println(String.format("%s == %s", retval, 325));

		retval = combination(26, 3);
		System.out.println(String.format("%s == %s", retval, 2600));

		retval = combination(52, 5);
		System.out.println(String.format("%s == %s", retval, 2598960));

		// we want to know the probability that one of the target bits is in the
		// open space
		// in the candidate.
		int N = 52;
		int k = 26;
		int n = 5;

		int x = 2;
		retval = combination(k, x) * combination((N - k), (n - x))
				/ combination(N, n);

		System.out.println(String.format("%s == %s", retval, 0.32513));

		retval = logCombination(k, x) + logCombination((N - k), (n - x))
				- logCombination(N, n);

		System.out.println(String.format("pow %s == %s",
				Math.pow(Math.E, retval), 0.32513));
		
		retval= hypergeometricProbability(N, k, n, x);
		System.out.println(String.format("hyper %s == %s", retval, 0.32513));

		// we want to know the probability that one of the target bits is in the
		// open space
		// in the candidate.
		N = 52;
		k = 13;
		n = 5;
		retval = 0.0;
		for (x = 0; x <= 2; x++) {
			double d = combination(k, x) * combination((N - k), (n - x))
					/ combination(N, n);
			System.out.println("Step " + x + " " + d);
			retval += d;
		}

		System.out.println(String.format("%s == %s", retval, 0.9072));
		
		retval= hypergeometricSeriesProbability(N, k, n, 0,2);
		System.out.println(String.format("hyper %s == %s", retval, 0.9072));
		
		System.out.println( getMissProbability(72, 24, 24 ));
		

		retval= hypergeometricSeriesProbability(72, 72-24, 24, 1, 24);
		System.out.println(String.format("hyper %s == %s", retval, 0.9072));
	}
}
