class Main {
  Void main() {
    Int a;
    Int b;
    a = 60;
    b = 50;
    println("The GCD of 60 and 50 is:");
    println(new Gcd().gcd(a, b)); // should print 10
  }
}

class Gcd {
  Int gcd(Int a, Int b) {
    while (a != b) {
      if (a > b) {
        a = a - b;
      } else {
        b = b - a;
      }
    }
    return a;
  }
}
