class Main {
  Void main() {
    Int op;
    Int index;
    String item;
    List list;

    list = new List();
    list.init();

    while (true) {
      println("Pick an operation:");
      println("1 = Add item");
      println("2 = Remove item");
      println("3 = Print list");
      println("4 = Exit");
      readln(op);

      if (op == 1) {
        println("Enter item to add");
        readln(item);
        list.add(item);
      } else {
        if (op == 2) {
          println("Enter index to remove");
          readln(index);
          list.removeAt(index);
        } else {
          if (op == 3) {
            list.print();
          } else {
            if (op == 4) {
              return;
            } else {
              if (op < 1 || op > 3) {
                println("Invalid operation");
              } else {
                op = op;
              }
            }
          }
        }
      }
    }
  }
}

class List {
  String item;
  List tail;
  Bool hasTail;

  Void init() {
    item = null;
    tail = null;
    hasTail = false;
  }

  Void add(String item) {
    List cur;
    cur = this;
    while (cur.hasTail) {
      cur = cur.tail;
    }
    cur.item = item;
    cur.tail = new List();
    cur.tail.init();
    cur.hasTail = true;
  }

  Void removeAt(Int index) {
    Int count;
    List cur;
    count = 0;
    cur = this;
    index = index - 1;
    while (cur.hasTail && count < index) {
      cur = cur.tail;
      count = count + 1;
    }
    if (cur.hasTail) {
      cur.hasTail = cur.tail.hasTail;
      cur.tail = cur.tail.tail;
    } else {
      // end of list has been reached
      return;
    }
  }


  Void print() {
    List cur;
    Int index;
    index = 0;
    cur = this;
    while (cur.hasTail) {
      println(index);
      println(cur.item);
      index = index + 1;
      cur = cur.tail;
    }
  }
}
