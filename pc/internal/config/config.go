package config

import (
	"io"

	"gopkg.in/yaml.v3"
)

type Config struct {
	ServerIp string `yaml:"server_ip"`
	TcpPort  int    `yaml:"tcp_port"`
}

func LoadConfig(r io.Reader) (*Config, error) {
	var cfg Config
	decoder := yaml.NewDecoder(r)
	err := decoder.Decode(&cfg)
	if err != nil {
		return nil, err
	}
	return &cfg, nil
}

func (cfg *Config) SaveConfig(w io.Writer) error {
	encoder := yaml.NewEncoder(w)
	err := encoder.Encode(cfg)
	_ = encoder.Close()
	if err != nil {
		return err
	}
	return nil
}
