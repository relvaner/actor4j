import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.Pair;

public class A {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		long start =System.currentTimeMillis();
		
		System.out.println(ObjectMapper.create().mapFrom(Pair.of(23423, 23)));
		System.out.println((System.currentTimeMillis()-start));
		start =System.currentTimeMillis();
		System.out.println(ObjectMapper.create().mapFrom(Pair.of(234723, 23)));
		System.out.println((System.currentTimeMillis()-start));
		start =System.currentTimeMillis();
		System.out.println(ObjectMapper.create().mapFrom(Pair.of(2342783, 23)));
		System.out.println((System.currentTimeMillis()-start));
		start =System.currentTimeMillis();
		System.out.println(ObjectMapper.create().mapFrom(Pair.of(2342663, 23)));
		System.out.println((System.currentTimeMillis()-start));
	}

}
