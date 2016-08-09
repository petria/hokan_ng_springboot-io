package org.freakz.hokan_ng_springboot.bot.ircengine;

import org.freakz.hokan_ng_springboot.bot.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.util.StringStuff;
import org.freakz.hokan_ng_springboot.bot.util.Uptime;
import org.jibble.pircbot.Colors;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * User: petria
 * Date: 21-Jan-2009
 * Time: 08:22:02
 */
public class WholeLineTrickers {

  private HokanCore core;

  public WholeLineTrickers(HokanCore hokanCore) {
    this.core = hokanCore;
  }

  private String _olpo = "";

  private void checkPerkeleVittu(IrcMessageEvent iEvent) {
    int rnd = 1 + (int) (Math.random() * 100);
    if (rnd < 85) {
      return;
    }
    String line = iEvent.getMessage().toLowerCase();
    if (line.contains("perkele")) {
      processReply(iEvent, _olpo + "PERKELE!");
    }
    if (line.contains("vittu")) {
      processReply(iEvent, _olpo + "(|)");
    }

  }

  private void checkHuomenta(IrcMessageEvent iEvent) {
    String line = iEvent.getMessage().toLowerCase();
    String reply = null;
    if (line.startsWith("aamut") ||
        line.startsWith("aamua") ||
        line.startsWith("huomenta") ||
        line.startsWith("huomenia") ||
        line.startsWith("houm") ||
        line.startsWith("ment") ||
        line.startsWith("hooment") ||
        line.startsWith("suolenta") ||
        line.startsWith("menta")) {

      Calendar cal = Calendar.getInstance();
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      if (hour >= 6 && hour <= 10) {
        if (line.startsWith("suolenta")) {
          reply = "*PRÖÖÖT*";
        } else {
          reply = "!!!!";
        }
      } else if (hour > 10 && hour <= 16) {
        reply = "Päivää!";
      } else if (hour > 16 && hour <= 23) {
        reply = "Iltaa!";
      } else if (hour > 23 && hour <= 5) {
        reply = "Yotä!";
      }
    }
    if (reply != null) {
      processReply(iEvent, _olpo + reply);
    }

  }

  private int jouluRandomBase = 65;

  private void checkJoulu(IrcMessageEvent iEvent) {

    String line = iEvent.getMessage();

    int rnd = 1 + (int) (Math.random() * 100);
    if (line.contains("joulu") && rnd > jouluRandomBase) {
      String[] format = {"00", "00", "00", "0"};

      GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
      cal.set(Calendar.MONTH, Calendar.DECEMBER);
      cal.set(Calendar.DAY_OF_MONTH, 24);
      cal.set(Calendar.YEAR, 2016);
      cal.set(Calendar.HOUR_OF_DAY, 12);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);

      Uptime uptime = new Uptime(cal.getTime().getTime());
      Integer[] ut = uptime.getTimeDiff();

      String ret = StringStuff.fillTemplate("%3 päivää ja %2:%1:%0 jouluun!", ut, format);

      processReply(iEvent, _olpo + ret);
      jouluRandomBase = 120;

    } else {
      jouluRandomBase--;
    }
  }

  private void checkJospa(IrcMessageEvent iEvent) {
    if (iEvent.getMessage().startsWith("jospa")) {
      String rndWord = "joo"; // TODO ChannelLogger.getInstance().getRandomWord();
      String reply = "Jospa " + rndWord;
      processReply(iEvent, _olpo + reply);
    }
  }

  public String[] splitByWord(String line, String word) {
    int idx = line.indexOf(" ");
    if (idx != -1) {
      line = line.substring(idx);
      return line.split(word);
    }
    return null;
  }

  private void checkPitasko(IrcMessageEvent iEvent) {
    String msg = iEvent.getMessage();
    if (StringStuff.match(iEvent.getMessage(), "pit.i?sk..*", true)) {
      StringBuilder sb = new StringBuilder();
      int rndLevel = 500;
      int rnd = (int) (Math.random() * 1000);

      if (iEvent.getMessage().toLowerCase().matches(".*(viina|viini|bisse|olut|viina|kalja|huikka|ryypätä|pillu|panna|perse).*")) {
        rndLevel = 5000;
      }
      if (iEvent.getMessage().toLowerCase().indexOf("linux") > 0) {
        rndLevel = 4000;
      }
      if (iEvent.getMessage().toLowerCase().matches(".*(mac|osx|win).*")) {
        rndLevel = 3000;
      }

      String rndWord = "";
      if (msg.contains("vai")) {
        String[] split = splitByWord(msg, "vai");
        rndWord = " " + StringStuff.getRandomString(split).trim() + " ";
      }


      if (rndLevel == 5000) {
        sb.append(String.format("Joo pitäis%s!!!!!!", rndWord));
      } else if (rndLevel == 4000) {
        sb.append(String.format("Joo pitäis%s!!!", rndWord));
      } else if (rndLevel == 3000) {
        sb.append("EI pitäis!!!");
      } else if (rnd > rndLevel) {
        sb.append(String.format("Joo pitäis%s!", rndWord));
      } else {
        sb.append("Ei pitäis!!");
      }
      processReply(iEvent, _olpo + sb.toString());

    }

  }

  public void checkApplause(IrcMessageEvent iEvent) {
    int rndLevel = 600;
    int rnd = (int) (Math.random() * 1000);

    if (rnd > rndLevel && iEvent.getMessage().contains("apple")) {
      processReply(iEvent, _olpo + iEvent.getSender() + ": IL masinointia!");
    }

  }

  public void checkSpede(IrcMessageEvent iEvent) {
    int rndLevel = 400;
    int rnd = (int) (Math.random() * 1000);

    if (rnd > rndLevel && iEvent.getMessage().toLowerCase().contains("spede")) {
      processReply(iEvent, _olpo + Colors.BOLD + "*voi rähmä - spedeläps*");
    }

  }

  public void checkSilli(IrcMessageEvent iEvent) {
    int rndLevel = 850;
    int rnd = (int) (Math.random() * 1000);
    String msg = iEvent.getMessage().toLowerCase();
    boolean kala = false;
    if (msg.contains("kala")) {
      kala = true;
    } else if (msg.contains("silli")) {
      kala = true;
    } else if (msg.contains("hauki")) {
      kala = true;
    } else if (msg.contains("ahven")) {
      kala = true;
    } else if (msg.contains("puikko")) {
      kala = true;
    } else if (msg.contains("turska")) {
      kala = true;
    }

    if (rnd > rndLevel && kala) {
      processReply(iEvent, _olpo + Colors.BOLD + "*2KG SIIKA!*");
    }
  }

  public void checkOlisko(IrcMessageEvent iEvent) {
    String msg = iEvent.getMessage().toLowerCase();
    if (msg.matches("olisko .*|oliskohan .*")) {
      int rnd = (int) (Math.random() * 1000);
      if (rnd > 500) {
        processReply(iEvent, _olpo + "Joo olis!");
      } else {
        processReply(iEvent, _olpo + "Ei olis!");
      }

    }
  }

  private final static String[] stonez1 = {"kivi", "paperi", "sakset"};
  private final static String[] stonez2 = {"paperi", "sakset", "kivi"};

  public void checkStonePaper(IrcMessageEvent iEvent) {
    String msg = iEvent.getMessage().toLowerCase();
    int fndIdx = -1;
    if (msg.equals(stonez1[0])) {
      fndIdx = 0;
    }
    if (msg.equals(stonez1[1])) {
      fndIdx = 1;
    }
    if (msg.equals(stonez1[2])) {
      fndIdx = 2;
    }

    if (fndIdx != -1) {
      int rnd = (int) (Math.random() * 1000) % 3;
      String my = stonez1[rnd];
      String reply = String.format("%s -> mulle %s - ", msg, my);
      if (msg.equals(my)) {
        reply += "tasapeli!!";
      } else if (my.equals(stonez2[fndIdx])) {
        reply += "voitin!";
      } else {
        reply += "hävisin!";

      }
      processReply(iEvent, _olpo + iEvent.getSender() + ": " + reply);
    }

  }


  public void checkMuisti(IrcMessageEvent iEvent) {
    String msg = iEvent.getMessage().toLowerCase();
    if (msg.startsWith("muista")) {
      processReply(iEvent, _olpo + iEvent.getSender() + ": muistissa!");
    }
  }

  private void checkHellsKitchen(IrcMessageEvent iEvent) {
    String msg = iEvent.getMessage().toLowerCase();
    if (msg.matches("ranskikset|chef|kitchen")) {
      processReply(iEvent, _olpo + iEvent.getSender() + ": SAATANA MIKSI SÄ TOIT NÄMÄ MULLE, ULOS TÄÄLTÄ, RANSKIKSET EI OLE MUSTIA!!!");
    }
  }

  private void processReply(IrcMessageEvent iEvent, String reply) {
    this.core.handleSendMessage(iEvent.getChannel(), reply);
  }

  private static long _lastReply = 0;

  public void checkWholeLineTrickers(IrcMessageEvent iEvent) {

    long now = new Date().getTime();
    long diff = now - _lastReply;

/*    if (iEvent.getHokanUser().isOlpo() > 0) {
      _olpo = "OLPO ";
    } else {
      _olpo = "";
    }*/

    //   checkApplause(iEvent);
    checkJospa(iEvent);
    checkPerkeleVittu(iEvent);
    checkJoulu(iEvent);
    checkPitasko(iEvent);
    checkSpede(iEvent);
    checkSilli(iEvent);
    checkOlisko(iEvent);

//    checkKickPattern(iEvent);
    checkStonePaper(iEvent);
    checkMuisti(iEvent);
    checkHellsKitchen(iEvent);

    if (diff > 1500) {

      checkHuomenta(iEvent);
      _lastReply = now;
    }

  }

}
