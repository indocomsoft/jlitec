class Main {
  Void main() {
    Int a;
    Int b;
    println("Enter first number");
    readln(a);
    println("Enter second number");
    readln(b);
    println(null);
    println("Numbers entered:");
    println(a);
    println(b);
    println(null);
    println("The GCD of the two numbers is:");
    println(new Gcd().gcd(a, b));
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
