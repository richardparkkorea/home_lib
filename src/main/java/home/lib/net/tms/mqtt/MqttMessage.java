package home.lib.net.tms.mqtt;

import java.nio.channels.SocketChannel;
import java.util.Arrays;

import home.lib.lang.UserException;
import home.lib.log.MyLogger;
import home.lib.util.DataStream;
import home.lib.util.StringUtil;

class MqttMessage {
	final public static int MaxBufferLength = 0x7f * 0x7f * 0x7f * 0x7f; //
	// public:
	final public static byte MqttError_MqttOk = 0;
	final public static byte MqttError_MqttNowhereToSend = 1;
	final public static byte MqttError_MqttInvalidMessage = 2;

	final public static byte Type_pUnknown = 0;
	final public static byte Type_Connect = 0x10;
	final public static byte Type_ConnAck = 0x20;
	final public static byte Type_Publish = 0x30;
	final public static byte Type_PubAck = 0x40;
	final public static byte Type_PubReceived = 0x50;
	final public static byte Type_PubRelease = 0x60;
	final public static byte Type_PubComplate = 0x70;

	final public static byte Type_Subscribe = (byte) 0x80;
	final public static byte Type_SubAck = (byte) 0x90;
	final public static byte Type_UnSubscribe = (byte) 0xA0;
	final public static byte Type_UnSuback = (byte) 0xB0;
	final public static byte Type_PingReq = (byte) 0xC0;
	final public static byte Type_PingResp = (byte) 0xD0;
	final public static byte Type_Disconnect = (byte) 0xE0;

	final public static byte State_FixedHeader = 0;
	final public static byte State_Length = 1;
	final public static byte State_VariableHeader = 2;
	final public static byte State_PayLoad = 3;
	final public static byte State_Complete = 4;
	final public static byte State_Error = 5;
	final public static byte State_Create = 6;

	long packet_identify = 0;

	DataStream buffer = new DataStream();
	byte vheader;
	int size = 0; // bytes left to receive
	int state;// State state;

	byte remaining_length_byte_index = 0;

	public MqttMessage() {
		reset();
	}

	public MqttMessage(byte t) {
		this(t, (byte) 0);
	}

	public MqttMessage(byte t, byte bits_d3_d0) {
		create(t);
		buffer.nativeBuf()[0] |= bits_d3_d0;

	}// MqttMessage(Type t, byte bits_d3_d0=0) { create(t); buffer[0] |= bits_d3_d0; }

	void incoming(byte in_byte) {

		buffer.writeByte(in_byte);

		switch (state) {
		case State_FixedHeader:
			size = 0;
			state = State_Length;
			remaining_length_byte_index = 0;
			break;
		case State_Length:

			// if (size == MaxBufferLength) {
			// size = in_byte & 0x7F; //first byte of remaining length
			//
			// } else {
			// remaining_length_byte_index++;
			// size += (in_byte & 0x7F) << (7*remaining_length_byte_index);
			// }
			if (remaining_length_byte_index == 0) {
				size = in_byte & 0x7F; // first byte of remaining length
				remaining_length_byte_index++;

			} else {

				size += (in_byte & 0x7F) << (7 * remaining_length_byte_index);
				remaining_length_byte_index++;
			}

			if (size > MaxBufferLength) {
				state = State_Error;

			} else if ((in_byte & (byte) 0x80) == 0) {
				vheader = (byte) buffer.length();

				if (size == 0) {

					state = State_Complete;

				} else {
					// System.out.println("buffer.reserve(size); - preallocate the buffer ");// buffer.reserve(size);
					state = State_VariableHeader;

					// System.out.format("vheader = %s size=%s buffer.len=%s buf=%s \n", vheader, size, buffer.length()
					// , StringUtil.ByteArrayToHex(buffer.copyOf()));
				}
			}
			break;
		case State_VariableHeader:
		case State_PayLoad:
			--size;
			if (size == 0) {
				state = State_Complete;
				// hexdump("rec");
			}
			break;
		case State_Create:
			size++;
			break;
		case State_Complete:
		default:
			// Serial << "Spurious " << _HEX(in_byte) << endl;
			// hexdump("spurious");
			// System.out.println("State_Complete");
			reset();
			break;
		}
		if (buffer.length() > MaxBufferLength) {
			// debug("Too long " << state);
			state = State_Error;
			reset();
		}
	}

	void add(byte bb) {
		incoming(bb);
	}

	void add(byte[] p, int len, boolean addLength) throws Exception // void add( byte[] p, int len, boolean
																	// addLength=true );
	{
		int limit = MqttMessage.MaxBufferLength;
		if (len >= limit)
			throw new UserException("payload size err( %s > %s )", len, limit);

		if (addLength) {
			// buffer.reserve(buffer.length()+2);
			// System.out.format("add.payload.len=%s \n", len);

			if (len >= (256 * 256))
				throw new UserException("payload size err2( %s > %s )", len, limit);

			incoming((byte) (len >> 8));
			incoming((byte) (len & 0xFF));
		}

		int n = 0;
		while ((len--) > 0)
			incoming(p[n++]);
	}

	void add(byte[] p, int len) throws Exception {
		add(p, len, true);
	}

	void add(String s) throws Exception {
		add(s.getBytes(), s.getBytes().length, true);
	}

	void add(Topic t) throws Exception {
		add(t.str());
	}

	// byte[] end() { return &buffer[0]+buffer.size(); }

	byte[] getVHeader() {

		// System.out.format("vheader = %s pick out(%s) total(%s) \r\n", vheader, (buffer.length() - vheader),
		// buffer.length());
		// return &buffer[vheader];
		return Arrays.copyOfRange(buffer.nativeBuf(), vheader, buffer.length());

	}

	void complete() {
		encodeLength();
	}

	void reset() {
		buffer.reset();
		state = State_FixedHeader;
		size = 0;
	}

	// buff is MSB/LSB/STRING
	// output buff+=2, len=length(str)
	// static void getString(const char* &buff, uint16_t& len);
	// static void getString( byte[] buff, int len)
	// {
	// len = (buff[0]<<8)|(buff[1]);
	// buff+=2;
	// }
	static int getPayload(int cur, byte[] buff, DataStream out) {

		// int len = (buff[0] << 8) | (buff[1]);

		int len = (buff[cur + 0] << 8) | (buff[cur + 1]);
		cur += 2;

		// System.out.format("getpayload.len=%s cur=%s buff.len=%s \n", len, cur, buff.length);

		byte[] b = Arrays.copyOfRange(buff, cur, cur + len);

		out.reset();
		out.writeBytes(b);
		out.setPos(0);

		cur += len;

		return cur;
	}

	public byte type() {
		// return state == Complete ? static_cast<Type>(buffer[0]) : Unknown;
		return ((state == State_Complete) ? buffer.byteAt(0) : Type_pUnknown);
	}

	// void create(byte type) {
	// buffer.reset();
	// buffer.writeByte((byte) type);// buffer = (char) type;
	// buffer.writeByte((byte) 0);// buffer += '\0'; // reserved for msg length byte 1/2
	// buffer.writeByte((byte) 0);// buffer += '\0'; // reserved for msg length byte 2/2 (fixed)
	// vheader = 3; // Should never change
	// size = 0;
	// state = State_Create;
	// }

	void create(byte type) {
		buffer.reset();
		buffer.writeByte((byte) type);// buffer = (char) type;
		buffer.writeByte((byte) 0);// buffer += '\0'; // reserved for msg length byte 1/4
		buffer.writeByte((byte) 0);// buffer += '\0'; // reserved for msg length byte 2/4 (fixed)
		buffer.writeByte((byte) 0);// buffer += '\0'; // reserved for msg length byte 3/4 (fixed)
		buffer.writeByte((byte) 0);// buffer += '\0'; // reserved for msg length byte 4/4 (fixed)
		vheader = 5; // Should never change
		size = 0;
		state = State_Create;
	}

	byte sendTo(MqttClient client)// MqttError sendTo(MqttClient*);

	{

		if (buffer.length() > 0) {
			// debug("sending " << buffer.size() << " bytes");
			encodeLength();
			// hexdump("snd");
			// client->write(&buffer[0], buffer.size());

			byte[] b = buffer.copyOf();

			MqttBroker.debug(MyLogger.DEBUG,
					" **send.len=" + b.length + " buf=" + StringUtil.omitAfter(StringUtil.ByteArrayToHex(b), 128));

			client.write(b);

		} else {
			// debug("??? Invalid send");
			return MqttError_MqttInvalidMessage;
		}
		return MqttError_MqttOk;
	}

	// void hexdump(byte[] prefix);// void hexdump( byte[] prefix=nullptr) ;

	// void encodeLength() {
	// if (state != State_Complete) {
	// int length = buffer.length() - 3; // 3 = 1 byte for header + 2 bytes for pre-reserved length field.
	// buffer.nativeBuf()[1] = (byte) (0x80 | (length & 0x7F));
	// buffer.nativeBuf()[2] = (byte) (length >> 7);
	// vheader = 3;
	//
	// // We could check that buffer[2] < 128 (end of length encoding)
	// state = State_Complete;
	// }
	// }
	//

	void encodeLength() {
		if (state != State_Complete) {
			int length = buffer.length() - 5; // 3 = 1 byte for header + 2 bytes for pre-reserved length field.
			buffer.nativeBuf()[1] = (byte) (0x80 | (length & 0x7F));
			buffer.nativeBuf()[2] = (byte) (0x80 | ((length >> 7) & 0x7F));
			buffer.nativeBuf()[3] = (byte) (0x80 | ((length >> 14) & 0x7F));
			buffer.nativeBuf()[4] = (byte) ((length >> 21) & 0x7F);
			vheader = 5;

			// We could check that buffer[2] < 128 (end of length encoding)
			state = State_Complete;

			// encodeLength_test(length);
		}
	}

	// void encodeLength_test(int X) {
	//
	// byte encodedByte = 0;
	// do {
	// encodedByte = (byte) (X % 128);
	// X = X / 128;
	// // if there are more data to encode, set the top bit of this byte
	// if (X > 0) {
	// encodedByte = (byte) (encodedByte | 128);
	// }
	// // endif
	// // 'output' encodedByte
	// System.out.format(" %x ", encodedByte);
	// } while (X > 0);
	//
	// }
	//
	// void decodeLength_test(byte[] ds, int p) {
	// int multiplier = 1;
	// int value = 0;
	// byte encodedByte;
	// do {
	// encodedByte = ds[p++];// 'next byte from stream'
	// value += (encodedByte & 127) * multiplier;
	// multiplier *= 128;
	//
	// if (multiplier > 128 * 128 * 128) {
	// System.out.println(" Error(Malformed Remaining Length)");
	// return;
	// }
	//
	// } while ((encodedByte & 128) != 0);
	// }

	public int length() {
		return buffer.length();
	}

	public MqttMessage copyOf() {

		MqttMessage m = new MqttMessage();

		m.buffer.writeBytes(this.buffer.copyOf());

		m.vheader = this.vheader;
		m.size = this.size;
		m.state = this.state;

		return m;

	}

	public void setDup(boolean b) {
		if (b)
			buffer.nativeBuf()[0] |= 0x08;// set dupFlag
		else
			buffer.nativeBuf()[0] &= 0xf7;// remove dupFlag

	}
};
