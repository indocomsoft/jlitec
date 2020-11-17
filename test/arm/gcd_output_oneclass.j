class Main {
  Void main() {
    Int a;
    Int b;
    a = 60;
    b = 45;
    println("Numbers entered:");
    println(a);
    println(b);
    println(null);
    while (a != b) {
      if (a > b) {
        a = a - b;
      } else {
        b = b - a;
      }
    }
    println("The GCD of the two numbers is:");
    println(a);
  }
}
