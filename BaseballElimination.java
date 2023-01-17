import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.ST;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.Stack;
public class BaseballElimination {
    private int numTeams;
    private ST<String,Integer> teamST;
    private ST<Integer,String> invertedTeamST;
    private ST<String,int[]> gameGrid;
    private ST<Integer,int[]> matchupST;
    private ST<String,Integer> winsST;
    private ST<String,Integer> lossesST;
    private ST<String,Integer> remainingST;
    private ST<Integer,Integer> againstST;
    private FlowNetwork flowNetwork;
    private int V;
    private int GV;
    private double G;
    private boolean trivialElimination = false;
    public BaseballElimination(String filename) {
        // create a baseball division from given filename in format specified below
        In fileIn = new In(filename);
        int row = 0;
        int teamIdx = 0;
        while (!fileIn.isEmpty()) {
            if (row == 0) {
                numTeams = fileIn.readInt();
                teamST = new ST<String,Integer>();
                invertedTeamST = new ST<Integer,String>();
                gameGrid = new ST<String,int[]>();
                winsST = new ST<String,Integer>();
                lossesST = new ST<String,Integer>();
                remainingST = new ST<String,Integer>();
            } else {
                String team = fileIn.readString();
                teamST.put(team,teamIdx);
                invertedTeamST.put(teamIdx,team);
                teamIdx += 1;
                int teamWins = fileIn.readInt();
                winsST.put(team,teamWins);
                int teamLosses = fileIn.readInt();
                lossesST.put(team,teamLosses);
                int teamGamesLeft = fileIn.readInt();
                remainingST.put(team,teamGamesLeft);
                int[] glRow = new int[numTeams];
                for (int i = 0; i < numTeams; i++) {
                    int opponentGamesLeft = fileIn.readInt();
                    glRow[i] = opponentGamesLeft;
                }
                gameGrid.put(team,glRow);
            }
            row += 1;
        }
    }                    
    public int numberOfTeams() {
        // number of teams
        return numTeams;
    }                     
    public Iterable<String> teams() {
        // all teams
        return teamST.keys();
    }                           
    public int wins(String team) {
        // number of wins for given team
        return winsST.get(team);
    }               
    public int losses(String team) {
        // number of losses for given team
        return lossesST.get(team);
    }                  
    public int remaining(String team) {
        // number of remaining games for given team
        return remainingST.get(team);
    }               
    public int against(String team1, String team2) {
        // number of remaining games between team1 and team2
        int[] againstRow = gameGrid.get(team1);
        int idx = teamST.get(team2);
        return againstRow[idx];

    }
    private void constructFlowNetwork(String team) {
        // construct the flow network for the given team;
        trivialElimination = false;
        for (String t : teams()) {
            if (!t.equals(team)) {
                if ((wins(team) + remaining(team)) < wins(t)) {
                    trivialElimination = true;
                }
            }
        }
        // StdOut.println("team: " + team);
        // StdOut.println("trivially eliminated?: " + trivialElimination);
        if (!trivialElimination) {
            G = 0.0;
            int numVertices = 2;
            int gameVertices = 0;
            int teamVertices = numTeams;
            againstST = new ST<Integer,Integer>();
            matchupST = new ST<Integer,int[]>();
            // Counting game vertices
            // StdOut.println("-----------------------");
            // StdOut.println("team: " + team);
            // StdOut.println("team placement: " + teamST.get(team));
            int i = 0;
            for (String t1 : teams()) {
                // StdOut.println("t1: " + t1);
                int j = 0;
                for (String t2 : teams()) {
                    if (j != i && j >= i && !t1.equals(team) && !t2.equals(team)) {
                        gameVertices += 1;
                        int[] matchup = new int[2];
                        matchup[0] = teamST.get(t1);
                        matchup[1] = teamST.get(t2);
                        againstST.put(gameVertices,against(t1,t2));
                        matchupST.put(gameVertices,matchup);
                    }
                    j += 1;
                }
                i += 1;
            }
            GV = gameVertices;
            numVertices += gameVertices + teamVertices;
            V = numVertices;
            flowNetwork = new FlowNetwork(numVertices);
            // Game and team vertex edges
            for (int v = 1; v <= gameVertices; v++) {
                int cap = againstST.get(v);
                double capacity = cap;
                G += capacity;
                FlowEdge gameEdge = new FlowEdge(0,v,capacity);
                flowNetwork.addEdge(gameEdge);
                int[] match = matchupST.get(v);
                FlowEdge teamEdge1 = new FlowEdge(v,match[0]+gameVertices+1,Double.POSITIVE_INFINITY);
                flowNetwork.addEdge(teamEdge1);
                FlowEdge teamEdge2 = new FlowEdge(v,match[1]+gameVertices+1,Double.POSITIVE_INFINITY);
                flowNetwork.addEdge(teamEdge2);
            }
            String t1 = team;
            int t_idx = 0;
            for (int t = gameVertices + 1; t < numVertices - 1; t++) {
                // StdOut.println("t2: " + t2);
                if (t != (teamST.get(t1)+gameVertices+1)) {
                    // StdOut.println("eligible teams: " + test);
                    String t2 = invertedTeamST.get(t_idx);
                    int c = wins(t1)+remaining(t1)-wins(t2);
                    FlowEdge finalEdge = new FlowEdge(t,numVertices-1,c);
                    flowNetwork.addEdge(finalEdge);
                }
                t_idx += 1;
            }
            // StdOut.println("------------------------");
            // StdOut.println("team: " + team);
            // StdOut.println(flowNetwork.toString());
        }
    }    
    public boolean isEliminated(String team) {
        // is given team eliminated?
        constructFlowNetwork(team);
        if (!trivialElimination) {
            FordFulkerson ff = new FordFulkerson(flowNetwork,0,V-1);
            // StdOut.println("team: " + team);
            // StdOut.println("max flow: " + ff.value());
            // StdOut.println("game vertices: " + G);
            if (ff.value() != G) {
                return true;
            }
            return false;
        }
        return true;
    } 
    public Iterable<String> certificateOfElimination(String team) {
        // subset R of teams that eliminates given team; null if not eliminated
        if (!isEliminated(team)) {
            return null;
        }
        Stack<String> cert = new Stack<String>();
        constructFlowNetwork(team);
        if (!trivialElimination) {
            FordFulkerson ff = new FordFulkerson(flowNetwork,0,V-1);
            int teamVertice = GV+1;
            for (String t : teams()) {
                if (!t.equals(team)) {
                    if (ff.inCut(teamVertice)) {
                        cert.push(t);
                    }
                    teamVertice += 1;
                }
            }
        } else {
            for (String t : teams()) {
                if (!t.equals(team) && (wins(team) + remaining(team)) < wins(t)) {
                    cert.push(t);
                }
            }
        }
        return cert;
    }
    public static void main(String[] args) {
        // BaseballElimination division = new BaseballElimination(args[0]);
        // for (String team : division.teams()) {
        //     if (division.isEliminated(team)) {
        //         StdOut.print(team + " is eliminated by the subset R = { ");
        //         for (String t : division.certificateOfElimination(team)) {
        //             StdOut.print(t + " ");
        //         }
        //         StdOut.println("}");
        //     }
        //     else {
        //         StdOut.println(team + " is not eliminated");
        //     }
        // }
    }
}