package jlitec.command;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public interface Command {
  String helpMessage();

  void setUpArguments(Subparser subparser);

  void run(Namespace parsed);
}
