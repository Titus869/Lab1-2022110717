package wordgraph;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class WordGraph {
	// —— 全局变量 —— //
	/** 用 LinkedHashMap 保证插入顺序（单词首次出现的顺序） */
	private static final Map<String, Map<String, Integer>> graph = new LinkedHashMap<>();
	/** 随机数，用于生成新文本时随机选桥接词 */
	private static final Random RANDOM = new Random();

	// 放在 WordGraph 类中（主类的某处，比如 main 上面）
	public static void buildGraphFromFile(String filePath) throws IOException {
		graph.clear(); // 清空旧图
		String text = Files.readString(Paths.get(filePath));
		String cleaned = text.replaceAll("[^a-zA-Z\\s]", " ").toLowerCase();
		String[] words = cleaned.split("\\s+");

		for (int i = 0; i < words.length - 1; i++) {
			String from = words[i], to = words[i + 1];
			if (from.isEmpty() || to.isEmpty())
				continue;
			graph.computeIfAbsent(from, k -> new LinkedHashMap<>()).merge(to, 1, Integer::sum);
		}
		graph.putIfAbsent(words[words.length - 1], new LinkedHashMap<>());
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("用法: java WordGraph <文件路径>");
			System.exit(1);
		}

		// —— 1. 读取文件并构建图 —— //
		buildGraphFromFile(args[0]);

		// —— 2. 交互式菜单 —— //
		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.println("\n--- Word Graph Menu ---");
			System.out.println("1. Show directed graph");
			System.out.println("2. Query bridge words");
			System.out.println("3. Generate new text");
			System.out.println("4. Calculate Shortest Path");
			System.out.println("5. Calculate PR");
			System.out.println("6. Rndom Walk");
			System.out.println("7. Exit");
			System.out.print("Choose an option: ");
			String choice = sc.nextLine().trim();

			switch (choice) {
			case "1" -> showDirectedGraph();
			case "2" -> {
				System.out.print("Enter word1: ");
				String w1 = sc.nextLine().trim();
				System.out.print("Enter word2: ");
				String w2 = sc.nextLine().trim();
				System.out.println(queryBridgeWords(w1, w2));
			}
			case "3" -> {
				System.out.print("Enter a line of text: ");
				String input = sc.nextLine();
				System.out.println("Generated text:");
				System.out.println(generateNewText(input));
			}
			case "4" -> {
				System.out.print("Enter source word: ");
				String w1 = sc.nextLine().trim();
				System.out.print("Enter target word: ");
				String w2 = sc.nextLine().trim();
				System.out.println(calcShortestPath(w1, w2));
			}
			case "5" -> {
				System.out.print("Enter word to calculate PageRank: ");
				String word = sc.nextLine().trim();
				Double rank = calPageRank(word);
				if (rank != null)
					System.out.printf("PageRank of \"%s\": %.6f%n", word, rank);
			}
			case "6" -> {
				System.out.println("Random walk starting...");
				randomWalk();
			}
			case "7" -> {
				System.out.println("Exiting.");
				sc.close();
				return;
			}

			default -> System.out.println("Invalid choice. Try again.");
			}
		}
	}

	/**
	 * 功能1：打印全局图的邻接表（带权重）。 因为 graph 是 LinkedHashMap，遍历 entrySet() 自然就是第一次插入的顺序。
	 */
	static void showDirectedGraph() {
		for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
			String from = entry.getKey();
			Map<String, Integer> edges = entry.getValue();
			if (edges.isEmpty())
				continue;

			List<String> parts = new ArrayList<>();
			for (Map.Entry<String, Integer> e : edges.entrySet()) {
				parts.add(e.getKey() + "(" + e.getValue() + ")");
			}
			System.out.println(from + " -> " + String.join(", ", parts));
		}
	}

	/**
	 * 功能2：查询两个单词间的桥接词。 只保留了两个参数，直接访问静态全局 graph。
	 */

	static String queryBridgeWords(String word1, String word2) {
		String w1 = word1.toLowerCase(), w2 = word2.toLowerCase();

		// 1. 检查输入单词是否存在
		if (!graph.containsKey(w1) || !graph.containsKey(w2)) {
			return "No " + w1 + " or " + w2 + " in the graph!";
		}

		// 2. 收集所有桥接词
		Set<String> bridges = new HashSet<>();
		for (String mid : graph.get(w1).keySet()) {
			Map<String, Integer> next = graph.get(mid);
			if (next != null && next.containsKey(w2)) {
				bridges.add(mid);
			}
		}

		// 3. 桥接词为空
		if (bridges.isEmpty()) {
			return "No bridge words from " + w1 + " to " + w2 + "!";
		}

		// 4. 排序，方便格式化输出
		List<String> list = new ArrayList<>(bridges);
		Collections.sort(list);

		// 5. 根据数量组合输出
		String formatted;
		switch (list.size()) {
		case 1 -> // 只有一个：直接输出它
			formatted = list.get(0);
		case 2 -> // 两个：用 " and " 连接
			formatted = list.get(0) + " and " + list.get(1);
		default -> {
			// 三个及以上：前 n-1 个用逗号分隔，最后两个之间加 ", and "
			String head = String.join(", ", list.subList(0, list.size() - 1));
			formatted = head + ", and " + list.get(list.size() - 1);
		}
		}

		return "The bridge words from " + w1 + " to " + w2 + " are: " + formatted + ".";
	}

	/**
	 * 功能3：根据桥接词生成新文本，在相邻词之间随机插入一个桥接词（如果存在）。
	 */
	static String generateNewText(String inputText) {
		String cleaned = inputText.replaceAll("[^a-zA-Z\\s]", " ").toLowerCase();
		String[] words = cleaned.split("\\s+");
		if (words.length == 0)
			return "";

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < words.length; i++) {
			sb.append(words[i]);
			if (i < words.length - 1) {
				String w1 = words[i], w2 = words[i + 1];
				Set<String> bridges = new HashSet<>();
				if (graph.containsKey(w1)) {
					for (String mid : graph.get(w1).keySet()) {
						Map<String, Integer> next = graph.get(mid);
						if (next != null && next.containsKey(w2)) {
							bridges.add(mid);
						}
					}
				}
				if (!bridges.isEmpty()) {
					List<String> list = new ArrayList<>(bridges);
					String pick = list.get(RANDOM.nextInt(list.size()));
					sb.append(" ").append(pick);
				}
				sb.append(" ");
			}
		}
		return sb.toString().trim();
	}

	/**
	 * 功能4:使用Dijkstra算法找到最短路径。
	 */
	static String calcShortestPath(String word1, String word2) {
		String start = word1.toLowerCase();
		String end = word2.toLowerCase();

		if (!graph.containsKey(start))
			return "No word \"" + word1 + "\" in the graph!";
		if (!graph.containsKey(end))
			return "No word \"" + word2 + "\" in the graph!";

		Map<String, Integer> dist = new HashMap<>();
		Map<String, String> prev = new HashMap<>();
		Set<String> visited = new HashSet<>();
		PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(dist::get));

		for (String node : graph.keySet()) {
			dist.put(node, Integer.MAX_VALUE);
		}
		dist.put(start, 0);
		queue.add(start);

		while (!queue.isEmpty()) {
			String curr = queue.poll();
			if (visited.contains(curr))
				continue;
			visited.add(curr);

			Map<String, Integer> neighbors = graph.getOrDefault(curr, Map.of());
			for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
				String neighbor = entry.getKey();
				int weight = entry.getValue();
				int newDist = dist.get(curr) + weight;
				if (newDist < dist.getOrDefault(neighbor, Integer.MAX_VALUE)) {
					dist.put(neighbor, newDist);
					prev.put(neighbor, curr);
					queue.add(neighbor);
				}
			}
		}

		if (!dist.containsKey(end) || dist.get(end) == Integer.MAX_VALUE) {
			return "No path from \"" + word1 + "\" to \"" + word2 + "\"!";
		}

		// Reconstruct path
		List<String> path = new LinkedList<>();
		for (String at = end; at != null; at = prev.get(at)) {
			path.add(0, at);
		}

		return "Shortest path from \"" + word1 + "\" to \"" + word2 + "\": " + String.join(" -> ", path)
				+ "\nTotal weight: " + dist.get(end);
	}

	/**
	 * 功能5：计算PageRank。
	 */
	static Double calPageRank(String word) {
		final double d = 0.85;
		final double threshold = 1e-6;
		final int maxIterations = 100;
		int N = graph.size();
		if (!graph.containsKey(word.toLowerCase())) {
			System.out.println("Word not found in graph.");
			return null;
		}

		// 初始化 PageRank 值
		Map<String, Double> pr = new HashMap<>();
		for (String node : graph.keySet()) {
			pr.put(node, 1.0 / N);
		}

		for (int iter = 0; iter < maxIterations; iter++) {
			Map<String, Double> newPr = new HashMap<>();
			for (String node : graph.keySet()) {
				newPr.put(node, (1 - d) / N);
			}

			// 处理正常出边
			for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
				String from = entry.getKey();
				Map<String, Integer> edges = entry.getValue();
				double currentPr = pr.get(from);

				if (edges.isEmpty()) {
					// 出度为0，将其PR均分给所有节点
					double share = currentPr / N;
					for (String node : newPr.keySet()) {
						newPr.put(node, newPr.get(node) + d * share);
					}
				} else {
					// 正常分发
					double totalWeight = edges.values().stream().mapToInt(i -> i).sum();
					for (Map.Entry<String, Integer> edge : edges.entrySet()) {
						String to = edge.getKey();
						int weight = edge.getValue();
						double share = (currentPr * weight) / totalWeight;
						newPr.put(to, newPr.get(to) + d * share);
					}
				}
			}

			// 检查是否收敛
			boolean converged = true;
			for (String node : pr.keySet()) {
				if (Math.abs(pr.get(node) - newPr.get(node)) > threshold) {
					converged = false;
					break;
				}
			}

			pr = newPr;
			if (converged)
				break;
		}

		return pr.get(word.toLowerCase());
	}

	/**
	 * 功能6：随机游走。
	 */
	static String randomWalk() {
		if (graph.isEmpty())
			return "Graph is empty.";

		List<String> nodes = new ArrayList<>(graph.keySet());
		String current = nodes.get(RANDOM.nextInt(nodes.size()));
		List<String> path = new ArrayList<>();
		Set<String> visitedEdges = new HashSet<>();

		path.add(current);
		Scanner userScanner = new Scanner(System.in);

		while (true) {
			Map<String, Integer> edges = graph.get(current);
			if (edges == null || edges.isEmpty())
				break;

			List<String> neighbors = new ArrayList<>(edges.keySet());
			String next = neighbors.get(RANDOM.nextInt(neighbors.size()));
			String edgeKey = current + "->" + next;

			if (visitedEdges.contains(edgeKey)) {
				break;
			}

			visitedEdges.add(edgeKey);
			current = next;
			path.add(current);

			// 提示用户是否中止
			System.out.print("Press Enter to continue, or type 'q' to stop: ");
			String line = userScanner.nextLine().trim();
			if (line.equalsIgnoreCase("q"))
				break;
		}
		userScanner.close();

		// 输出并写入文件
		String result = String.join(" -> ", path);
		System.out.println("Random walk path: " + result);

		try (PrintWriter writer = new PrintWriter("output.txt")) {
			writer.println(result);
		} catch (IOException e) {
			System.out.println("Failed to write to file: " + e.getMessage());
		}

		return result;
	}

}
