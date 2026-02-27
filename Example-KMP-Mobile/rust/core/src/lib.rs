pub fn rust_greeting(name: &str) -> String {
    format!("Hello from Rust! 🦀 {} is running on Rust v{}", name, env!("CARGO_PKG_VERSION"))
}

pub fn fibonacci(n: u32) -> u64 {
    match n {
        0 => 0,
        1 => 1,
        _ => {
            let mut a: u64 = 0;
            let mut b: u64 = 1;
            for _ in 2..=n {
                let temp = a + b;
                a = b;
                b = temp;
            }
            b
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_fibonacci() {
        assert_eq!(fibonacci(0), 0);
        assert_eq!(fibonacci(1), 1);
        assert_eq!(fibonacci(10), 55);
        assert_eq!(fibonacci(20), 6765);
    }

    #[test]
    fn test_greeting() {
        let result = rust_greeting("Test");
        assert!(result.contains("Rust"));
        assert!(result.contains("Test"));
    }
}
