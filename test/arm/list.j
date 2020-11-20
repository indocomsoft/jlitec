class Main {
    Void main() {
        Int op;
        Int index;
        String item;
        List list;

        list = new List();
        list.init();

        println("====");
        println("Adding");
        println("====");
        list.add("asd");
        list.add("qwe");
        list.add("zxc");
        list.print();
        println("====");
        println("Remove 1");
        println("====");
        list.removeAt(1);
        list.print();
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

    // Void removeAt(Int index) {
    //     Int count;
    //     List cur;
    //     count = 0;
    //     cur = this;
    //     index = index - 1;
    //     while (cur.hasTail && count < index) {
    //         cur = cur.tail;
    //         count = count + 1;
    //     }
    //     if (count < index || !cur.hasTail) {
    //         return;
    //     } else {
    //         // splice cur.tail from the list
    //         cur.hasTail = cur.tail.hasTail;
    //         cur.tail = cur.tail.tail;
    //     }
    // }

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
