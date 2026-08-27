# Audio Classifier

This project is a voice-based identification system that identifies a person based on their voice.

The application records a person's voice and converts the recording into a **speaker embedding**. These embeddings are stored for registered users and are later compared with the embedding generated from a new voice recording. The person whose stored embedding is most similar to the new embedding is identified as the speaker.

## Models and Libraries Used

### TitaNet Large

**TitaNet Large** is the model used to generate speaker embeddings from audio.

- Model: `titanet_large.onnx`
- Approximate size: 100 MB
- Used in this project for: **Speaker embedding generation**

### Sherpa-ONNX

**Sherpa-ONNX** is the Android library used to run the TitaNet Large model and provide the speaker embedding extraction functionality.

- Library: `sherpa-onnx.aar`
- Approximate size: 50 MB
- Used in this project for: **Running the model and generating speaker embeddings**

## Approach

The project follows a simple approach: a user's voice is recorded and converted into an embedding, which is a numerical representation of the voice. This embedding is saved for registered users. When a new person speaks, their voice is converted into another embedding and compared with the stored embeddings. The closest match is used to identify the speaker.

## Detailed Documentation

For the complete technical documentation, see:

[Audio Classifier – Detailed Documentation](https://app.notion.com/p/Audio-Classifier-3c8ba74c24af80cdbf70f53703bcd609?source=copy_link)
