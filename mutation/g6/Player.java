package mutation.g6;

import java.util.*;
import mutation.sim.Console;
import mutation.sim.Mutagen;

public class Player extends mutation.sim.Player {
    private Random random;
    private int[] beforeCounter;
    private int[] afterCounter;
    private Map<Character, Integer> hash;
    private Map<Integer, Character> antiHash;
    private Map<String, Set<String>> cumLeft;
    private Map<String, Integer> cumRight;
    private int maxCount;
    private String mostOutput;
    private int numMutation;
    
    public Player() {
        random = new Random();
        hash = new HashMap<>();
        hash.put('a', 0); hash.put('c', 1); hash.put('g', 2); hash.put('t', 3);
        antiHash = new HashMap<>();
        antiHash.put(0, 'a'); antiHash.put(1, 'c'); antiHash.put(2, 'g'); antiHash.put(3, 't');
        cumLeft = new HashMap<>();
        cumRight = new HashMap<>();
        maxCount = 0;
        mostOutput = "";
    }

    private String randomString() {
        char[] pool = {'a', 'c', 'g', 't'};
        String result = "";
        for (int i = 0; i < 1000; ++ i)
            result += pool[Math.abs(random.nextInt() % 4)];
        return result;
    }

    @Override
    public Mutagen Play(Console console, int m) {
        Mutagen result = new Mutagen();
        //result.add("a;c;c", "att");
        //result.add("g;c;c", "gtt");
        for (int i = 0; i < 100; ++ i) {
            String genome = randomString();
            String mutated = console.Mutate(genome);
            char[] input = genome.toCharArray();
            char[] output = mutated.toCharArray();
            Element[] diff = checkDifference(input, output);
            result = getNaive(diff);
            numMutation = console.getNumberOfMutations();
            console.Guess(result);
        }
        return result;
    }

    public Mutagen getNaive(Element[] diff) {
        Mutagen result = new Mutagen();
        List<Window> winList = new ArrayList<>();
        for(int i = 0; i < 1000; i++) {
        	if(diff[i].isMutated()) {
        		Window temp = new Window(i, i+9, diff);
        		winList.add(temp);
        		i+=10;
        	}
        }
        if (winList.isEmpty())
            return result;
        Window temp = winList.get(0);
        Set<String> left = new HashSet<>();
        int length = getLength(winList.get(0));
        
        String output;
        if(numMutation <= 7) output = getWinInt(winList);
        else output = getWinInt7(winList);

        int curr = cumRight.getOrDefault(output, 0) + 1;
        cumRight.put(output, curr);
        if(curr > maxCount) {
            maxCount = curr;
            mostOutput = output;
        }
        
        String leftOne = "";

        getLeftHelper(winList, left, output);

        if(curr == 1) cumLeft.put(output, new HashSet<String>());
        cumLeft.get(output).addAll(left); 

        left = cumLeft.get(mostOutput);
        for(int i = 0; i < length; i++) {
            Set<Character> c = new HashSet<>();
            for(String s: left) {
                if(i < s.length())
                    c.add(s.charAt(i));
            }
            if(c.size() != 0) {
                leftOne += combine(c);
                if(i != length -1) leftOne += ";";
            }
        }

        System.out.println("output: " + output);
        System.out.println("maxOutput: " + output);
        result.add(leftOne, mostOutput);    
        return result;
    }

    public void getLeftHelper(List<Window> winList, Set<String> s, String output) { 
        HashMap<Integer, Set<String>> map = new HashMap<>();
        int[] count = new int[19];

        //System.out.println("output: " + output);

        for(Window w: winList) {
            String left = w.getAfterString();
            //System.out.println("right: " + left);
            if(!LCSubStr(left, output, left.length(), output.length()).equals(output)) continue;
            Element[] e = w.getWindow();
            String out = "";
            int start = -1;
            if(output == "") continue;
            for(int i = 0; i < 19; i++) {
                if(e[i].getAfter() == output.charAt(0)) {
                    start = i;
                    out += Character.toString(e[i].getOG());
                    boolean sub = true;
                    for(int j = 1; j < output.length(); j++) {
                        if(i+j > 18) {
                            start = -1;
                            break;
                        }
                        if(e[j+i].getAfter() != output.charAt(j)) {
                            start = -1;
                            sub = false;
                            break;
                        }
                        else out += Character.toString(e[j+i].getOG());
                    }
                    if(!sub) {
                        start = -1;
                        out = "";
                    }
                }
            }
            if(start == -1) continue;
            Set<String> temp = map.getOrDefault(start, new HashSet<>());
            temp.add(out);
            map.put(start, temp);
            count[start]++;
        }
        int maxIndex = 0;
        int max = 0;
        for(int i = 0; i < 19; i++) {
            if(count[i] > max) {
                max = count[i];
                maxIndex = i;
            }
        }
        if(max == 0) return;
        //System.out.println("Max: " + maxIndex);
        s.addAll(map.get(maxIndex));
    }

    public String getLeft(Window w, String output) {
        String left = w.getAfterString();
        if(!LCSubStr(left, output, left.length(), output.length()).equals(output)) return "";
        //System.out.println("before: " + left);

    	Element[] e = w.getWindow();
    	String out = "";
    	if(output == "") return "";
    	for(int i = 0; i < 19; i++) {
    		if(e[i].getAfter() == output.charAt(0)) {
    			out += Character.toString(e[i].getOG());
    			boolean sub = true;
    			for(int j = 1; j < output.length(); j++) {
    				if(i+j > 18) break;
    				if(e[j+i].getAfter() != output.charAt(j)) {
    					sub = false;
    					break;
    				}
    				else out += Character.toString(e[j+i].getOG());
    			}
    			if(sub) return out;
    			else out = "";
    		}
    	}
    	return out;
    }
    //if m <= 7
    public String getWinInt(List<Window> list){
        String output = list.get(0).getAfterString();
        for(Window w: list) {
            String other = w.getAfterString();
            output = LCSubStr(output, other, output.length(), other.length());
        }
        //System.out.println(output);
    	return output;
    }

    //if m > 7
    public String getWinInt7(List<Window> list) {
        HashMap<String, Integer> map = new HashMap<>();
        int max = 0;
        String output = "";
        for(int i = 0; i < list.size() - 1; i++) {
            String outside = list.get(i).getAfterString();
            for(int j = 0; j < list.size(); j++) {
                String inside = list.get(j).getAfterString();
                String temp = LCSubStr(outside, inside, outside.length(), inside.length());
                int currCount = map.getOrDefault(temp, 0) + 1;
                if(currCount > max) {
                    max = currCount;
                    output = temp;
                }
                //System.out.println(temp);
                map.put(temp, currCount);
            }
        }
        //System.out.println("final: " + output);
        return output;
    }

    public String getLeftInt(List<Window> list){
        String output = list.get(0).getAfterString();
        for(Window w: list) {
            String other = w.getBeforeString();
            output = LCSubStr(output, other, output.length(), other.length());
        }
        //System.out.println(output);
        return output;
    }

    public String combine(Set<Character> input) {
        String output = "";
        for(char c: input) {
            output += Character.toString(c);
        }
        return output;
    }

    public String putSemi(String s) {
        String output = "";
        for(int i = 0; i < s.length(); i++) {
            output = output + Character.toString(s.charAt(i));
            if(i != s.length()-1) output += ";";
        }
        return output;
    }

    public int getLength(Window w) {
        return w.mutEnd - w.mutStart + 1;
    }

    public Element[] checkDifference(char[] input, char[] output) {
        Element[] diff = new Element[1000];
        beforeCounter = new int[4];
        afterCounter = new int[4];
        for(int i = 0; i < input.length; i++) {
            if(input[i] != output[i]) {
                diff[i] = new Element(true, input[i], output[i]);
                beforeCounter[hash.get(input[i])]++;
                afterCounter[hash.get(output[i])]++;
            }
            else diff[i] = new Element(input[i]);
        }
        return diff;
    }

    private String LCSubStr(String X, String Y, int m, int n)
    {
        int[][] LCSuff = new int[m + 1][n + 1];
        int len = 0;
        int row = 0, col = 0;

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0)
                    LCSuff[i][j] = 0;

                else if (X.charAt(i - 1) == Y.charAt(j - 1)) {
                    LCSuff[i][j] = LCSuff[i - 1][j - 1] + 1;
                    if (len < LCSuff[i][j]) {
                        len = LCSuff[i][j];
                        row = i;
                        col = j;
                    }
                }
                else
                    LCSuff[i][j] = 0;
            }
        }

        if (len == 0) {
            return "";
        }

        String resultStr = "";
        while (LCSuff[row][col] != 0) {
            resultStr = X.charAt(row - 1) + resultStr; // or Y[col-1]
            --len;

            row--;
            col--;
        }

        return resultStr;
    }

    public class Element {
        private boolean mutated;
        private char og;
        private char after;

        public Element() {
            mutated = false;
        }

        public Element(char og) {
            mutated = false;
            this.og = og;
            this.after = og;
        }

        public Element(boolean mutated, char og, char after) {
            this.mutated = mutated;
            this.og = og;
            this.after = after;
            if(!this.mutated) this.after = og;
        }

        public boolean isMutated() {
            return mutated;
        }

        public char getOG() {
            return og;
        }

        public char getAfter() {
            return after;
        }

        public void putOG(char og) {
            this.og = og;
        }

        public void putAfter(char after) {
            this.after = after;
        }
    }

    public class Window {
    	public int start;
    	public int end;
        public int mutStart;
        public int mutEnd;
    	public int mutagenCount;
    	public Element[] window;

    	public Window() {

    	}

    	public Window(int left, int right, Element[] input) {
    		start = left;
    		end = right;
            mutStart = -1;
            mutEnd = -1;
    		mutagenCount = 0;
    		window = new Element[19];
    		int index = 0;

    		for(int i = left-9+1000; i <= right+1000; i++) {
    			window[index++] = input[i%1000];
    			if(input[i%1000].isMutated()) {
                    if(mutStart == -1) mutStart = index-1;
    				mutagenCount++;
                    mutEnd = index-1;
    			}
    		}
    	}

    	public Element[] getWindow() {
    		return window;
    	}

    	public int getMutagenCount() {
    		return mutagenCount;
    	}

    	public boolean isSameLoc(Window temp) {
    		for(int i = 0; i < 10; i++) {
    			if(temp.window[i].isMutated() && this.window[i].isMutated()) {
    				continue;
    				
    			} else return false;
    		}
    		return true;
    	}

        public String getAfter(){
            String temp = "";
            //System.out.println("mutStart!: " + mutStart);
            //System.out.println("mutEnd!: " + mutEnd);
            for(int i = mutStart; i <= mutEnd; i++) {
                temp = temp.concat(Character.toString(window[i].getAfter()));
            }
            return temp;
        }

        public String getAfterString() {
            String temp = "";
            //System.out.println("mutStart!: " + mutStart);
            //System.out.println("mutEnd!: " + mutEnd);
            for(int i = 0; i < 19; i++) {
                temp = temp.concat(Character.toString(window[i].getAfter()));
            }
            return temp;
        }

        public String getBeforeString() {
            String temp = "";
            //System.out.println("mutStart!: " + mutStart);
            //System.out.println("mutEnd!: " + mutEnd);
            for(int i = 0; i < 19; i++) {
                temp = temp.concat(Character.toString(window[i].getOG()));
            }
            return temp;
        }


        public String getOG(){
            String temp = "";
            boolean first = true;
            for(int i = mutStart; i <= mutEnd; i++) {
                temp = temp.concat(Character.toString(window[i].getOG()));
            }
            return temp;
        }
    }


}


