package main;

import java.util.ArrayList;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import data.GameData;
import data.RemoveLaser;
import data.ShipData;
import data.Upgrade;

public class World{
	private ArrayList<Player> players;
	public ArrayList<Ship> ships;
	public ArrayList<Ship> sortedships;
	private ArrayList<Base> bases;
	private ArrayList<Laser> lasers;
	private int radius;
	private transient GameData gamedata;
	private transient Server server;
	private transient Timer gametimer;
	public static int GAMETIMER = 150;
	public long sendtime;
	public static final int SMALL = 1;
	public static final int MEDIUM = 2;
	public static final int LARGE = 3;
//	public static int flush;
	public World(int size, Server s) {
		server = s;
		players = new ArrayList<Player>();
		bases = new ArrayList<Base>();
		ships = new ArrayList<Ship>();
		sortedships = new ArrayList<Ship>();
		lasers = new ArrayList<Laser>();
		setSize(size);
		gamedata = new GameData();
		gametimer= new Timer(this.GAMETIMER, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				while(true) {
					System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
					long currenttime = System.currentTimeMillis();
					for(int a=0; a<ships.size(); a++) {
						Ship s = ships.get(a);
						if(s==null) {
							ships.remove(a--);
							continue;
						}
						if(s.hasTarget()) {
							Rectangle move = s.getMoveX();
							if(!collides(s, move)) {
								s.setPos(move);
							}
							move = s.getMoveY();
							if(!collides(s, move)) {
								s.setPos(move);
							}
						}
						s.servertic();
						if(s.laserReady()) {
							Thing en = getClosestEnemy(s);
							
							
							if(en!=null && s.canShoot(en)) {
								s.shot();
								Laser l = new Laser(s.getID(), en.getID(), s.getCooldown()-4, s.getDamage());
								l.source = s.source;
								lasers.add(l);
//								if(flush++>10) {
//									server.flush();
//									flush = 0;
//								}
								server.sendToAll(l);
								
							}
						}
		//				ships.get(a).move();
					}
					long shiptime = System.currentTimeMillis()-currenttime;
					for(int a=0; a<bases.size(); a++) {
						Base b = bases.get(a);
						b.tic();
						if(b.ready()) {
							Ship spawn = b.getShip();
							Rectangle bounds = getSpace(spawn, b);
							if(bounds==null) {
							} else {
								spawn.setPos(bounds);
								addShip(spawn);
								b.resetTimer();
							}
						}
					}
					long basetime = System.currentTimeMillis()-currenttime-shiptime;
					for(int a=lasers.size()-1; a>=0; a--) {
						Laser l = lasers.get(a);
						if(l.widen()) {
							Thing en = getThing(l.to);
							if(en!=null) {
								if(en.takeDamage(l.damage)) {
									removeThing(en);
									Base b = l.source;
									if(b!=null) {
										b.addMoney(en.getLoot());
									}
								}
							}
							if(a<lasers.size())
								lasers.remove(a);
						}
					}
					long lasertime = System.currentTimeMillis()-currenttime - basetime;
					sendGameData((int) (World.GAMETIMER-lasertime));
					sendtime = System.currentTimeMillis()-currenttime - lasertime;
					long delta = System.currentTimeMillis()-currenttime;
					System.out.println("Shiptime:"+shiptime+",  Basetime:"+basetime+",  Lasertime:"+lasertime+",  Sendtime:"+sendtime);
					System.out.println("Num Ships: "+ships.size()+",   Time: "+delta);
					
					while(System.currentTimeMillis()-currenttime<World.GAMETIMER) {
						//waiting for at least set time to pass
					}
				}
			}
		});
	}
//	public class Timer implements Runnable{
//		private Thread mythread;
//		public Timer() {
//			mythread = new Thread(this);
//		}
//		public void start() {
//			mythread.start();
//		}
//		@Override
//		public void run() {
//			while(true) {
//				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
//				long currenttime = System.currentTimeMillis();
//				for(int a=0; a<ships.size(); a++) {
//					Ship s = ships.get(a);
//					if(s==null) {
//						ships.remove(a--);
//						continue;
//					}
//					if(s.hasTarget()) {
//						Rectangle move = s.getMoveX();
//						if(!collides(s, move)) {
//							s.setPos(move);
//						}
//						move = s.getMoveY();
//						if(!collides(s, move)) {
//							s.setPos(move);
//						}
//					}
//					s.servertic();
//					if(s.laserReady()) {
//						Thing en = getClosestEnemy(s);
//						
//						
//						if(en!=null && s.canShoot(en)) {
//							s.shot();
//							Laser l = new Laser(s.getID(), en.getID(), s.getCooldown()-4, s.getDamage());
//							l.source = s.source;
//							lasers.add(l);
////							if(flush++>10) {
////								server.flush();
////								flush = 0;
////							}
//							server.sendToAll(l);
//							
//						}
//					}
//	//				ships.get(a).move();
//				}
//				long shiptime = System.currentTimeMillis()-currenttime;
//				for(int a=0; a<bases.size(); a++) {
//					Base b = bases.get(a);
//					b.tic();
//					if(b.ready()) {
//						Ship spawn = b.getShip();
//						Rectangle bounds = getSpace(spawn, b);
//						if(bounds==null) {
//						} else {
//							spawn.setPos(bounds);
//							addShip(spawn);
//							b.resetTimer();
//						}
//					}
//				}
//				long basetime = System.currentTimeMillis()-currenttime-shiptime;
//				for(int a=lasers.size()-1; a>=0; a--) {
//					Laser l = lasers.get(a);
//					if(l.widen()) {
//						Thing en = getThing(l.to);
//						if(en!=null) {
//							if(en.takeDamage(l.damage)) {
//								removeThing(en);
//								Base b = l.source;
//								if(b!=null) {
//									b.addMoney(en.getLoot());
//								}
//							}
//						}
//						if(a<lasers.size())
//							lasers.remove(a);
//					}
//				}
//				long lasertime = System.currentTimeMillis()-currenttime - basetime;
//				sendGameData((int) (World.GAMETIMER-lasertime));
//				sendtime = System.currentTimeMillis()-currenttime - lasertime;
//				long delta = System.currentTimeMillis()-currenttime;
//				System.out.println("Shiptime:"+shiptime+",  Basetime:"+basetime+",  Lasertime:"+lasertime+",  Sendtime:"+sendtime);
//				System.out.println("Num Ships: "+ships.size()+",   Time: "+delta);
//				
//				while(System.currentTimeMillis()-currenttime<World.GAMETIMER) {
//					//waiting for at least set time to pass
//				}
//			}
//		}
//	}
	public Rectangle getSpace(Ship spawn, Base base) {
		int a=0;
		int b = 0;
		Rectangle bounds = new Rectangle(0, 0, spawn.getWidth(), spawn.getHeight());
		for(int asdf = 1; asdf<5; asdf++) {
			bounds.y = base.getY()-spawn.getHeight()*asdf;
			for(a=base.getX()-spawn.getWidth()*asdf; a<=base.getX()+base.getWidth()+spawn.getWidth()*(asdf-1); a+=5) {
				bounds.x = a;
				if(!collides(spawn, bounds)) {
					return bounds;
				}
			}
			bounds.x = base.getX()+base.getWidth()+spawn.getWidth()*(asdf-1);
			for(a=base.getY()-spawn.getHeight()*asdf; a<=base.getY()+base.getHeight()+spawn.getHeight()*(asdf-1); a+=5) {
				bounds.y = a;
				if(!collides(spawn, bounds)) {
					return bounds;
				}
			}
			bounds.y = base.getY()+base.getHeight()+spawn.getHeight()*(asdf-1);
			for(a=base.getX()+base.getWidth()+spawn.getWidth()*(asdf-1); a>=base.getX()-spawn.getWidth()*asdf; a-=5) {
				bounds.x = a;
				if(!collides(spawn, bounds)) {
					return bounds;
				}
			}
			bounds.x = base.getX()-spawn.getWidth()*asdf;
			for(a=base.getY()+base.getHeight()+spawn.getHeight()*(asdf-1); a>=base.getY()-spawn.getHeight()*asdf; a-=5) {
				bounds.y = a;
				if(!collides(spawn, bounds)) {
					return bounds;
				}
			}
		}
		return null;
	}
	public void removeThing(Thing t) {
		if(t instanceof Ship) {
			removeShip((Ship)t);
		} else if (t instanceof Base) {
			//Need to add stuff here
		}
		
	}
	public void removeShip(Ship s) {
		ships.remove(s);
		sortedships.remove(s);
		ShipData sd = s.getData();
		sd.dead = true;
		server.sendToAll(sd);
		for(int a=lasers.size()-1; a>=0; a--) {
			Laser l = lasers.get(a);
			if(l.from==s.getID()) {
				lasers.remove(a);
				RemoveLaser rl = new RemoveLaser(l);
				server.sendToAll(rl);
			}
		}
	}
	public Thing getThing(int id) {
		for(int a=0; a<ships.size(); a++) {
			if(ships.get(a).getID()==id) {
				return ships.get(a);
			}
		}
		for(int a=0; a<bases.size(); a++) {
			if(bases.get(a).getID()==id) {
				return bases.get(a);
			}
		}
		return null;
	}
//	public Ship getShip(int id) {
//		for(int a=0; a<ships.size(); a++) {
//			if(ships.get(a).getID()==id) {
//				return ships.get(a);
//			}
//		}
//		return null;
//	}
	public Thing getRandomCloseEnemy(Ship s) {
		Ship en = null;
		int dist = 99999;
		for(int a = 0; a<sortedships.size(); a++) {
			Ship b = sortedships.get(a);
			if(!b.getPlayer().equals(s.getPlayer())) {
				int d = b.getDistanceFrom(s);
				if(d<dist) {
					dist = d;
					en = b;
					if(Math.random()<.1) {
						return en;
					}
				}
			}
		}
		return en;
	}
	public Thing getClosestEnemy(Ship s) {
		Thing en = null;
		int dist = 99999;
		for(int a = 0; a<sortedships.size(); a++) {
			Ship b = sortedships.get(a);
			if(!b.getPlayer().equals(s.getPlayer())) {
				int d = b.getDistanceFrom(s);
				if(d<dist) {
					dist = d;
					en = b;
				}
			}
		}
		for(int a=0; a<bases.size(); a++) {
			Base b = bases.get(a);
			if(!b.getDead() && !b.getPlayer().equals(s.getPlayer())) {
				int d = b.getDistanceFrom(s);
				if(d<dist) {
					dist = d;
					en = b;
				}
			}
		}
		return en;
	}
	/**
	 * Checks whether Rectangle newpos collides with any other thing in the World except Ship s
	 * @param s
	 * @param newpos
	 * @return true if collision, false, if clear
	 */
	public boolean collides(Ship s, Rectangle newpos) {
		for(int a=0; a<ships.size(); a++) {
			Ship sh = ships.get(a);
			if(sh==null)
				System.out.println("SH is null!!");
			if(sh!=s && sh.collides(newpos)) {
				return true;
			}
		}
		for(int a=0; a<bases.size(); a++) {
			Base ba = bases.get(a);
			if(ba==null) {
				System.out.println("BA is null!!");
			}
			if(ba.collides(newpos)) {
				return true;
			}
		}
		return false;
	}
	public void playerMoveCommand(Player p, int x, int y) {
		for(int a=0; a<sortedships.size(); a++) {
			Ship s = sortedships.get(a);
			if(s.getPlayer().equals(p)) {
				s.setTarget(new Point(x, y));
			}
		}
	}

	public void playerUpgradeCommand(Player player, Upgrade u) {
		Base b = this.getBase(player);
		if(b==null) {
			return;
		}
		b.upgrade(u);
	}
	public Base getBase(Player p) {
		for(int a=0; a<this.bases.size(); a++) {
			if(bases.get(a).getPlayer().equals(p)) {
				return bases.get(a);
			}
		}
		return null;
	}
	public void addShip(Ship s) {
		ships.add(s);
		sortedships.add(s);
	}
	public void sendGameData(int timeremaining) {
		long cur = System.currentTimeMillis();
		for(int a=0; a<bases.size(); a++) {
			server.sendToAll(bases.get(a));
			server.sendToPlayer(bases.get(a).getPlayer(), bases.get(a).getUpgrades());
		}
		if(ships.size()>0) {
			int size = ships.size();
			int a=0;
			for(; a<ships.size() && timeremaining>0; a++) {
				Ship s = ships.remove(0);
				if(s.sent) {
					server.sendToAll(s.getData());
				} else {
					server.sendToAll(s.getDetailedData());
					s.sent = true;
				}
				ships.add(s);
				timeremaining-=(System.currentTimeMillis()-cur);
			}
			System.out.println("Timeremaining:"+timeremaining+",  sent: "+a+"/"+size);
			
		}
	}
	public void startGame() {
		gametimer.start();
	}
	public void initializeBases() {
		int numbases = players.size();
		double radperbase = 2*Math.PI/numbases;
		int playerindex = 0;
		
		for(double rad = 0; rad<2*Math.PI && playerindex<numbases; rad = rad+radperbase, playerindex++) {
			int[] center = polartorect(radius-40, rad);
			Base b = new Base(players.get(playerindex), center[0], center[1], 80, 80);
			b.addMoney(server.startingmoney);
			bases.add(b);
		}
	}
	public void addPlayer(Player p) {
		players.add(p);
	}
	public void setSize(int size) {
		radius = getRadius(size);
	}
	public static int getRadius(int size) {
		if(size==SMALL) {
			return 500;
		} else if(size == MEDIUM) {
			return 1000;
		} else if(size == LARGE) {
			return 1500;
		}
		return 0;
	}
	public String mytoString() {
		String s = "World( radius="+radius+" players="+players.size()+"\n";
		for(int a=0; a<bases.size(); a++) {
			s+="Base("+bases.get(a).toString()+")\n";
		}
		for(int a=0; a<ships.size(); a++) {
			s+="Ship("+ships.get(a).toString()+")\n";
		}
		return s;
	}
	public static int[] polartorect(double radius, double angle) {
		int[] toret = new int[2];
		toret[0] = (int) (Math.cos(angle)*radius);
		toret[1] = (int) (Math.sin(angle)*radius);
		return toret;
	}
	public static double[] recttopolar(double x, double y) {
		double[] toret = new double[2];
		toret[0] = Math.sqrt(x*x+y*y);
		toret[1] = Math.atan2(y, x);
		return toret;
	}
	public static Color getOposite(Color c) {
		return new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue());
	}
}
