package jlitec.command;

import net.sourceforge.argparse4j.inf.Namespace;

public interface Command {
  void run(Namespace parsed);
}
