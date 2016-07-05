import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

class Player {
	static int arenaWidth = 16000;
	static int arenaHeigh = 9000;
	static Point[] corners = new Point[] { new Point(0, 0), new Point(arenaWidth, arenaHeigh) };

	public static void main(String[] args) throws IOException {
		new Environment().play();
	}

	static class Environment {
		Game game;
		MyReader reader;
		Bot playerBot = new DirectBot();

		/* read intialization */
		public void init(MyReader reader) throws IOException {
			int bustersPerPlayer = reader.nextInt();
			int ghostCount = reader.nextInt();
			int myTeamId = reader.nextInt();
			game.state = new State(myTeamId, bustersPerPlayer * 2, ghostCount);

			// System.err.println("Init :" + " bustersPerPlayer: " +
			// bustersPerPlayer + " " + " ghostCount: " + ghostCount
			// + " " + " myTeamId: " + myTeamId);

			// divide to sections
			divideArenaToSections(game, bustersPerPlayer);

		}

		private void divideArenaToSections(Game game2, int bustersPerPlayer) {
			game.sections = new Section[bustersPerPlayer + 3];
			double sectionAngle = Math.PI / (2 * bustersPerPlayer);
			for (int i = 0; i < game.sections.length - 3; i++) {
				double angle = i * sectionAngle + sectionAngle / 2;
				double x = 16000;
				double y = Math.tan(angle) * x;
				if (y > 9000) {
					y = 9000;
					x = y / Math.tan(angle);
				}
				game.sections[i] = new Section();
				game.sections[i].pos = new Point((int) x, (int) y);
				if (game.state.playerId == 1) {
					game.sections[i].pos.x = 16000 - game.sections[i].pos.x;
					game.sections[i].pos.y = 9000 - game.sections[i].pos.y;
				}
				System.err.println("Section : " + i + "  " + game.sections[i].pos.x + "  " + game.sections[i].pos.y);
			}
			// top left
			game.sections[bustersPerPlayer] = new Section();
			game.sections[bustersPerPlayer].pos = new Point(arenaWidth, 0);
			System.err.println("Section : " + bustersPerPlayer + "  " + game.sections[bustersPerPlayer].pos.x + "  "
					+ game.sections[bustersPerPlayer].pos.y);

			// corner
			game.sections[bustersPerPlayer + 1] = new Section();
			List<Point> intersection = Utils.getCircleLineIntersectionPoint(corners[game.state.playerId],
					corners[1 - game.state.playerId], corners[1 - game.state.playerId], 1601);
			if (intersection.size() == 2) {
				if (intersection.get(0).x >= 0 && intersection.get(0).x <= 16000 && intersection.get(0).y >= 0
						&& intersection.get(0).y <= 9000) {
					game.sections[bustersPerPlayer + 1].pos = intersection.get(0);
				} else {
					game.sections[bustersPerPlayer + 1].pos = intersection.get(1);
				}
			} else {
				game.sections[bustersPerPlayer + 1].pos = intersection.get(0);
			}
			System.err.println("Section : " + (bustersPerPlayer + 1) + "  " + game.sections[bustersPerPlayer + 1].pos.x
					+ "  " + game.sections[bustersPerPlayer + 1].pos.y);

			// bottom right
			game.sections[bustersPerPlayer + 2] = new Section();
			game.sections[bustersPerPlayer + 2].pos = new Point(0, arenaHeigh);
			System.err.println("Section : " + (bustersPerPlayer + 2) + "  " + game.sections[bustersPerPlayer + 2].pos.x
					+ "  " + game.sections[bustersPerPlayer + 2].pos.y);
		}

		/* read input */
		public void input(MyReader reader) throws IOException {
			List<Ghost> ghostsInput = new ArrayList<>();
			List<Buster> bustersInput = new ArrayList<>();
			int n = reader.nextInt();
			// System.err.print("Input :" + " entities: " + n);
			for (int i = 0; i < n; i++) {
				int entityId = reader.nextInt(), x = reader.nextInt(), y = reader.nextInt(),
						entityType = reader.nextInt(), state = reader.nextInt(), value = reader.nextInt();
				// System.err.print(" entityId :" + entityId + " x: " + x + " y:
				// " + y + " entityType: " + entityType
				// + " state: " + state + " value: " + value);
				// System.err.println();
				if (entityType == -1) {
					if (game.state.ghosts[entityId] == null) {
						game.state.ghosts[entityId] = new Ghost();
					}
					game.state.ghosts[entityId].pos = new Point(x, y);
					game.state.ghosts[entityId].nbBusters = value;
					game.state.ghosts[entityId].stamnia = state;

					Ghost ghostInput = new Ghost();
					ghostInput.pos = game.state.ghosts[entityId].pos;
					ghostInput.id = entityId;
					ghostsInput.add(ghostInput);
				} else {
					if (game.state.busters[entityId] == null) {
						game.state.busters[entityId] = new Buster();
					}
					game.state.busters[entityId].pos = new Point(x, y);
					game.state.busters[entityId].team = entityType;
					if (state != 2) {
						game.state.busters[entityId].cGhostId = value;
					}
					game.state.busters[entityId].state = state;

					Buster busterInput = new Buster();
					busterInput.id = entityId;
					busterInput.pos = new Point(x, y);
					bustersInput.add(busterInput);
				}
			}

			/* regulate ghosts position */
			for (int j = 0; j < game.state.ghosts.length; j++) {
				if (game.state.ghosts[j] != null) {
					boolean exist = false;
					for (int i = 0; i < ghostsInput.size(); i++) {
						if (ghostsInput.get(i).id == j) {
							exist = true;
							break;
						}
					}
					for (int i = 0; i < game.state.busters.length; i++) {
						if (game.state.busters[i] != null) {
							if (!exist && game.state.busters[i].team == game.state.playerId && Utils
									.distBetween(game.state.busters[i].pos, game.state.ghosts[j].pos) <= 2200 * 2200) {
								game.state.ghosts[j] = null;
								break;
							}
						}
					}
				}
			}

			/* regulate buster position */
			for (int j = 0; j < game.state.busters.length; j++) {
				if (game.state.busters[j] != null && game.state.busters[j].team != game.state.playerId) {
					boolean exist = false;
					for (int i = 0; i < bustersInput.size(); i++) {
						if (bustersInput.get(i).id == j) {
							exist = true;
							break;
						}
					}
					for (int i = 0; i < game.state.busters.length; i++) {
						if (game.state.busters[i] != null) {
							if (!exist && game.state.busters[i].team == game.state.playerId && Utils
									.distBetween(game.state.busters[i].pos, game.state.busters[j].pos) <= 2200 * 2200) {
								game.state.busters[j] = null;
								break;
							}
						}
					}
				}
			}

		}

		/* get action to perform */
		public void getAction() {
			Action[] actions = playerBot.updateBusters(game);
			playerBot.simulateGame(game, actions);
			game.state.actions.add(actions);
		}

		/* print action */
		public void output(PrintStream ps) {
			Action[] actions = game.state.actions.get(game.state.actions.size() - 1);
			for (int i = 0; i < actions.length; i++) {
				if (actions[i] != null) {
					ps.println(actions[i].action);
				}
			}
		}

		/* play */
		public void play() throws IOException {
			game = new Game();
			reader = new MyReader();
			init(reader);
			while (true) {
				input(reader);
				getAction();
				output(System.out);
			}
		}
	}

	static class Game {
		int roundNb;
		State state;
		Section[] sections;
	}

	static class State {
		int playerId;
		Buster[] busters;
		Ghost[] ghosts;
		List<Action[]> actions = new ArrayList<>();

		State(int playerId, int nbBusters, int nbGhosts) {
			this.playerId = playerId;
			busters = new Buster[nbBusters];
			ghosts = new Ghost[nbGhosts];
		}
	}

	static class Action {
		int busterId;
		String action = "";
	}

	static class MoveAction extends Action {
		Point pos = new Point();

		public MoveAction(int x, int y) {
			this.pos.x = x;
			this.pos.y = y;
			this.action = "MOVE " + x + " " + y;
		}
	}

	static class StunAction extends Action {
		int busterId;

		public StunAction(int busterId) {
			this.busterId = busterId;
			this.action = "STUN " + busterId;
		}
	}

	static class BustAction extends Action {
		int ghostId;

		public BustAction(int ghostId) {
			this.ghostId = ghostId;
			this.action = "BUST " + ghostId;
		}
	}

	static class ReleaseAction extends Action {
		public ReleaseAction() {
			this.action = "RELEASE";
		}
	}

	static class Buster {
		int id;
		Point pos;
		int team;
		int state;
		int cGhostId = -1;
		int fGhostId = -1;
		int sectionId = -1;
		int stunRound = -1;
	}

	static class Ghost {
		int id;
		Point pos;
		int nbBusters;
		int cBusterId = -1;
		int fBusterId = -1;
		int stamnia;
	}

	static class Section {
		Point pos;
		int fBusterId = -1;
	}

	/* AI */
	interface Bot {
		Action[] updateBusters(Game game);

		void simulateGame(Game game, Action[] actions);
	}

	static class GeneticBot implements Bot {
		@Override
		public Action[] updateBusters(Game game) {
			return null;
		}

		@Override
		public void simulateGame(Game game, Action[] actions) {

		}
	}

	static class DirectBot implements Bot {
		@Override
		public Action[] updateBusters(Game game) {
			Action[] actions = new Action[game.state.busters.length / 2];
			int index = 0;
			for (int i = 0; i < game.state.busters.length; i++) {
				if (game.state.busters[i] != null && game.state.busters[i].team == game.state.playerId) {
					int ghostId = -1;
					double dist = Double.MAX_VALUE;
					int stamnia = Integer.MAX_VALUE;
					int busterId = -1;
					if (game.roundNb > 8) {
						if (game.state.busters[i].stunRound == -1
								|| game.roundNb - game.state.busters[i].stunRound >= 20) {
							for (int j = 0; j < game.state.busters.length; j++) {
								if (game.state.busters[j] != null && game.state.busters[j].team != game.state.playerId
										&& game.state.busters[j].state != 2
										&& Utils.distBetween(game.state.busters[j].pos,
												game.state.busters[i].pos) <= 1760 * 1760) {
									busterId = j;
									if (game.state.busters[j].cGhostId != -1) {
										break;
									}
								}
							}
						}
						if (busterId == -1 && game.state.busters[i].cGhostId == -1
								|| (game.state.busters[i].cGhostId != -1
										&& game.state.ghosts[game.state.busters[i].cGhostId] != null)) {
							// if (busterId == -1) {
							if (game.state.busters[i].cGhostId != -1
									&& game.state.ghosts[game.state.busters[i].cGhostId] != null) {
								ghostId = game.state.busters[i].cGhostId;
								dist = Utils.distBetween(game.state.ghosts[ghostId].pos, game.state.busters[i].pos);
							}
							for (int j = 0; j < game.state.ghosts.length; j++) {
								if (game.state.ghosts[j] != null && game.state.ghosts[j].stamnia <= stamnia
								/*
								 * &&
								 * Utils.distBetween(game.state.ghosts[j].pos,
								 * game.state.busters[i].pos) < dist
								 */
										&& Utils.distBetween(game.state.ghosts[j].pos, game.state.busters[i].pos) >= 900
												* 900) {
									if (stamnia == game.state.ghosts[j].stamnia) {
										if (Utils.distBetween(game.state.ghosts[j].pos,
												game.state.busters[i].pos) < dist) {
											ghostId = j;
											dist = Utils.distBetween(game.state.ghosts[j].pos,
													game.state.busters[i].pos);
										}
									} else {
										stamnia = game.state.ghosts[j].stamnia;
										ghostId = j;
										dist = Utils.distBetween(game.state.ghosts[j].pos, game.state.busters[i].pos);
									}

								}
							}
							// }
						}
					}
					if (ghostId != -1) {
						if (dist <= 1760 * 1760 && dist >= 900 * 900) {
							actions[index] = new BustAction(ghostId);
							actions[index].busterId = i;
							// if (game.state.ghosts[ghostId].stamnia == 1) {
							// game.state.ghosts[ghostId].cBusterId = i;
							// game.state.busters[i].cGhostId = ghostId;
							// game.state.ghosts[ghostId] = null;
							// }
						} else {
							actions[index] = new MoveAction(game.state.ghosts[ghostId].pos.x,
									game.state.ghosts[ghostId].pos.y);
							actions[index].busterId = i;
							game.state.ghosts[ghostId].fBusterId = i;
							game.state.busters[i].fGhostId = ghostId;
						}
					} else if (busterId == -1) {
						int sectionId = -1;
						double distToSection = Double.MAX_VALUE;
						if (game.roundNb <= 8) {
							// get nearest sections
							for (int j = 0; j < game.sections.length - 3; j++) {
								boolean taken = false;
								for (int k = 0; k < game.state.busters.length; k++) {
									if (game.state.busters[k] != null && k != i
											&& game.state.busters[k].sectionId == j) {
										taken = true;
										break;
									}
								}
								if (!taken && Utils.distBetween(game.state.busters[i].pos,
										game.sections[j].pos) < distToSection) {
									sectionId = j;
									distToSection = Utils.distBetween(game.state.busters[i].pos, game.sections[j].pos);
								}
							}
						} else {
							// get farthest section
							if (game.state.busters[i].cGhostId == -1 && (game.state.busters[i].sectionId == -1
									|| (game.state.busters[i].pos.x == game.sections[game.state.busters[i].sectionId].pos.x
											&& game.state.busters[i].pos.y == game.sections[game.state.busters[i].sectionId].pos.y))) {
								for (int j = 0; j < game.sections.length; j++) {
									if (Utils.distBetween(game.state.busters[i].pos,
											game.sections[j].pos) > distToSection) {
										sectionId = j;
										distToSection = Utils.distBetween(game.state.busters[i].pos,
												game.sections[j].pos);
									}
								}
							}
						}
						if (sectionId == -1 && game.state.busters[i].sectionId != -1
								&& game.state.busters[i].cGhostId == -1) {
							sectionId = game.state.busters[i].sectionId;
						}
						if (sectionId != -1) {
							actions[index] = new MoveAction(game.sections[sectionId].pos.x,
									game.sections[sectionId].pos.y);
							actions[index].busterId = i;
							game.sections[sectionId].fBusterId = i;
							game.state.busters[i].sectionId = sectionId;
							System.err.println("Section : " + i + " " + game.state.busters[i].sectionId);
						} else {
							if (game.state.busters[i].cGhostId == -1) {
								actions[index] = new MoveAction(arenaWidth / 2, arenaHeigh / 2);
								actions[index].busterId = i;
							} else {
								if (Utils.distBetween(corners[game.state.playerId], game.state.busters[i].pos) <= 1600
										* 1600) {
									actions[index] = new ReleaseAction();
									actions[index].busterId = i;
									game.state.ghosts[game.state.busters[i].cGhostId] = null;
								} else {
									// nearest point to the corner
									List<Point> intersection = Utils.getCircleLineIntersectionPoint(
											game.state.busters[i].pos, corners[game.state.playerId],
											corners[game.state.playerId], 1598);
									if (intersection.size() == 2) {
										if (intersection.get(0).x >= 0 && intersection.get(0).x <= 16000
												&& intersection.get(0).y >= 0 && intersection.get(0).y <= 9000) {
											actions[index] = new MoveAction(intersection.get(0).x,
													intersection.get(0).y);
											actions[index].busterId = i;
										} else {
											actions[index] = new MoveAction(intersection.get(1).x,
													intersection.get(1).y);
											actions[index].busterId = i;
										}
									} else {
										actions[index] = new MoveAction(intersection.get(0).x, intersection.get(0).y);
										actions[index].busterId = i;
									}
								}
							}
						}
					} else {
						actions[index] = new StunAction(busterId);
						actions[index].busterId = i;
						game.state.busters[i].stunRound = game.roundNb;
						game.state.busters[busterId].state = 2;
					}
					index++;
				}
			}
			game.roundNb++;
			return actions;
		}

		@Override
		public void simulateGame(Game game, Action[] actions) {
			if (game.roundNb != 1) {

				// move ghost
				for (int j = 0; j < game.state.ghosts.length; j++) {
					if (game.state.ghosts[j] != null) {
						int busterId = -1;
						double dist = 2200 * 2200;
						for (int k = 0; k < game.state.busters.length; k++) {
							if (game.state.busters[k] != null
									&& Utils.distBetween(game.state.busters[k].pos, game.state.ghosts[j].pos) <= dist) {
								dist = Utils.distBetween(game.state.busters[k].pos, game.state.ghosts[j].pos);
								busterId = k;
							}
						}
						if (busterId != -1) {
							moveGhost(game, j, busterId);
						}
						System.err.println("Ghost " + j + " : " + game.state.ghosts[j].pos);
					}
				}

			}

			// apply actions
			for (int i = 0; i < actions.length; i++) {
				if (actions[i] instanceof MoveAction) {
					MoveAction mAction = (MoveAction) actions[i];
					// move buster
					moveBuster(game, mAction);
				}
			}
		}

		private void moveBuster(Game game, MoveAction mAction) {
			Point nextPoint;
			if (Utils.distBetween(mAction.pos, game.state.busters[mAction.busterId].pos) >= 800 * 800) {
				List<Point> intersection = Utils.getCircleLineIntersectionPoint(
						game.state.busters[mAction.busterId].pos, mAction.pos, game.state.busters[mAction.busterId].pos,
						800);
				if (intersection.size() == 2) {
					if ((int) Utils.angleBtw(game.state.busters[mAction.busterId].pos, intersection.get(0),
							mAction.pos) == 0) {
						nextPoint = intersection.get(0);
					} else {
						nextPoint = intersection.get(1);
					}
				} else {
					nextPoint = intersection.get(0);
				}
			} else {
				nextPoint = mAction.pos;
			}

			// regulate position
			regulatePosition(game.state.busters[mAction.busterId].pos, nextPoint, 800);
		}

		private void moveGhost(Game game, int j, int busterId) {
			Point nextPoint;
			if (Utils.distBetween(game.state.busters[busterId].pos, game.state.ghosts[j].pos) > 0) {
				List<Point> intersection = Utils.getCircleLineIntersectionPoint(game.state.busters[busterId].pos,
						game.state.ghosts[j].pos, game.state.ghosts[j].pos, 400);

				if (intersection.size() == 2) {
					if ((int) Utils.angleBtw(game.state.ghosts[j].pos, intersection.get(0),
							game.state.busters[busterId].pos) != 0
							&& (intersection.get(0).x != game.state.busters[busterId].pos.x
									|| intersection.get(0).y != game.state.busters[busterId].pos.y)) {

						nextPoint = intersection.get(0);
					} else {
						nextPoint = intersection.get(1);
					}
				} else {
					nextPoint = intersection.get(0);
				}
			} else {
				nextPoint = game.state.ghosts[j].pos;
			}

			// regulate position
			regulatePosition(game.state.ghosts[j].pos, nextPoint, 400);

		}

		private void regulatePosition(Point objPos, Point nextPoint, int radius) {
			if (nextPoint.y < 0 && nextPoint.x >= 0 && nextPoint.x <= 16000) {
				nextPoint.y = 0;
				// double tmp = Math.sqrt(radius * radius - objPos.y *
				// objPos.y);
				// if (nextPoint.x >= objPos.x) {
				// nextPoint.x = (int) (tmp + objPos.x);
				// } else {
				// nextPoint.x = (int) (-tmp + objPos.x);
				// }
			}

			if (nextPoint.y > 9000 && nextPoint.x >= 0 && nextPoint.x <= 16000) {
				nextPoint.y = 9000;
				// double tmp = Math.sqrt(radius * radius - (9000 - objPos.y) *
				// (9000 - objPos.y));
				// if (nextPoint.x >= objPos.x) {
				// nextPoint.x = (int) (tmp + objPos.x);
				// } else {
				// nextPoint.x = (int) (-tmp + objPos.x);
				// }
			}

			if (nextPoint.x < 0 && nextPoint.y >= 0 && nextPoint.y <= 9000) {
				nextPoint.x = 0;
				// double tmp = Math.sqrt(radius * radius - objPos.x *
				// objPos.x);
				// if (nextPoint.y >= objPos.y) {
				// nextPoint.y = (int) (tmp + objPos.y);
				// } else {
				// nextPoint.y = (int) (-tmp + objPos.y);
				// }
			}

			if (nextPoint.x > 16000 && nextPoint.y >= 0 && nextPoint.y <= 9000) {
				nextPoint.x = 16000;
				// double tmp = Math.sqrt(radius * radius - (16000 - objPos.x) *
				// (16000 - objPos.x));
				// if (nextPoint.y >= objPos.y) {
				// nextPoint.y = (int) (tmp + objPos.y);
				// } else {
				// nextPoint.y = (int) (-tmp + objPos.y);
				// }
			}

			if (nextPoint.x != 0 || nextPoint.y != 0) {

				// if (nextPoint.x >= 16000 && nextPoint.y <= 0) {
				// nextPoint.x = 16000;
				// nextPoint.y = 0;
				// }
				//
				// if (nextPoint.x <= 0 && nextPoint.y >= 9000) {
				// nextPoint.x = 0;
				// nextPoint.y = 9000;
				// }
				//
				// if (nextPoint.x <= 0 && nextPoint.y <= 0) {
				// nextPoint.x = 0;
				// nextPoint.y = 0;
				// }
				//
				// if (nextPoint.x >= 16000 && nextPoint.y >= 9000) {
				// nextPoint.x = 1600;
				// nextPoint.y = 9000;
				// }
				// update position
				objPos.x = nextPoint.x;
				objPos.y = nextPoint.y;
			}
		}
	}

	static class Utils {
		static double distBetween(Point p1, Point p2) {
			return (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y);
		}

		public static List<Point> getCircleLineIntersectionPoint(Point pointA, Point pointB, Point center,
				double radius) {
			double baX = pointB.x - pointA.x;
			double baY = pointB.y - pointA.y;
			double caX = center.x - pointA.x;
			double caY = center.y - pointA.y;

			double a = baX * baX + baY * baY;
			double bBy2 = baX * caX + baY * caY;
			double c = caX * caX + caY * caY - radius * radius;

			double pBy2 = bBy2 / a;
			double q = c / a;

			double disc = pBy2 * pBy2 - q;
			if (disc < 0) {
				return Collections.emptyList();
			}
			// if disc == 0 ... dealt with later
			double tmpSqrt = Math.sqrt(disc);
			double abScalingFactor1 = -pBy2 + tmpSqrt;
			double abScalingFactor2 = -pBy2 - tmpSqrt;

			Point p1 = new Point((int) (pointA.x - baX * abScalingFactor1), (int) (pointA.y - baY * abScalingFactor1));
			if (disc == 0) { // abScalingFactor1 == abScalingFactor2
				return Collections.singletonList(p1);
			}
			Point p2 = new Point((int) (pointA.x - baX * abScalingFactor2), (int) (pointA.y - baY * abScalingFactor2));
			return Arrays.asList(p1, p2);
		}

		static public double angleBtw(Point a, Point b, Point c) {
			Point ab = new Point(b.x - a.x, b.y - a.y);
			Point ac = new Point(c.x - a.x, c.y - a.y);
			double dot_prod = ab.x * ac.x + ab.y * ac.y;
			return Math.acos(dot_prod / (Math.sqrt(ab.x * ab.x + ab.y * ab.y) * Math.sqrt(ac.x * ac.x + ac.y * ac.y)));
		}

	}

	static class MyReader {

		static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		static StringTokenizer st;
		static PrintWriter out;

		public static String next() throws IOException {
			while (st == null || !st.hasMoreTokens()) {
				st = new StringTokenizer(br.readLine());
			}
			return st.nextToken();
		}

		public static int nextInt() throws IOException {
			return Integer.parseInt(next());
		}

		public static long nextLong() throws IOException {
			return Long.parseLong(next());
		}

		public static float nextFloat() throws IOException {
			return Float.parseFloat(next());
		}

		public static double nextDouble() throws IOException {
			return Double.parseDouble(next());
		}

	}

}
