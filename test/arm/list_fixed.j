class Main {
  Void main() {
    List list;

    list = new List();
    list.init();

    list.add("never");
    list.add("gonna");
    list.add("give");
    list.add("you");
    list.add("up");
    list.add("420");
    list.add("blaze");
    list.add("it");
    list.print();
    /* Prints:
0
Item = never
1
Item = gonna
2
Item = give
3
Item = you
4
Item = up
5
Item = 420
6
Item = blaze
7
Item = it
     */
    println("Removing");
    list.removeAt(5);
    list.removeAt(0);
    list.removeAt(2);
    list.print();
    /* Prints:
Removing
0
Item = gonna
1
Item = give
2
Item = up
3
Item = blaze
4
Item = it
     */
    println("Adding, then printing with spaces");
    list.add("!");
    list.printWithSpaces();
    /* Prints:
gonna give up blaze it !
    */
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
    if (index == 0 ) {
      if (this.hasTail) {
        this.item = this.tail.item;
        this.hasTail = this.tail.hasTail;
        this.tail = this.tail.tail;
      } else {
        return;
      }
    } else {
      count = 0;
      cur = this;
      index = index - 1;
      while (cur.hasTail && count < index) {
        cur = cur.tail;
        count = count + 1;
      }
      if (cur.tail.hasTail) {
        cur.hasTail = cur.tail.hasTail;
        cur.tail = cur.tail.tail;
      } else {
        // end of list has been reached
        return;
      }
    }
  }


  Void print() {
    List cur;
    Int index;
    index = 0;
    cur = this;
    while (cur.hasTail) {
      println(index);
      println("Item = " + cur.item);
      index = index + 1;
      cur = cur.tail;
    }
  }

  Void printWithSpaces() {
    List cur;
    String result;
    if (this.hasTail) {
      result = this.item;
      cur = this.tail;
      while (cur.hasTail) {
        result = result + " " + cur.item;
        cur = cur.tail;
      }
      println(result);
    } else {
      return;
    }
  }
}
